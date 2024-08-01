/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.{AssessmentFormProvider, AssessmentFormProvider2}
import logging.Logging
import models.AssessmentAnswer.NotAnsweredYet
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.{AssessmentPage, AssessmentPage2}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, CategorisationDetailsQuery2, RecategorisingQuery}
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AssessmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssessmentController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AssessmentFormProvider,
  formProvider2: AssessmentFormProvider2,
  categorisationService: CategorisationService,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad2(recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      request.userAnswers.get(CategorisationDetailsQuery2(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map{ assessment =>
            val listItems = assessment.getExemptionListItems
            val form = formProvider2(listItems.size)

            val preparedForm = request.userAnswers.get(AssessmentPage2(recordId, index)) match {
              case Some(value) => form.fill(value)
              case None => form
            }

            Ok(view(preparedForm, NormalMode, recordId, index, listItems, categorisationInfo.commodityCode))
          }
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val categorisationResult = for {
        userAnswersWithCategorisations <- categorisationService.requireCategorisation(request, recordId)
        categorisationInfo <-
          Future.fromTry(Try(userAnswersWithCategorisations.get(CategorisationDetailsQuery(recordId)).get))
        listItems = categorisationInfo.categoryAssessments(index).getExemptionListItems
        commodityCode = categorisationInfo.commodityCode
        exemptions = categorisationInfo.categoryAssessments(index).exemptions
        category = categorisationInfo.categoryAssessments(index).category
        form = formProvider(exemptions.size)
        preparedForm = userAnswersWithCategorisations.get(AssessmentPage(recordId, index)) match {
          case Some(value) => form.fill(value)
          case None => form
        }
      } yield {
        val areWeRecategorising = request.userAnswers.get(RecategorisingQuery(recordId)).getOrElse(false)

        val hasAssessmentBeenAnswered =
          request.userAnswers.get(AssessmentPage(recordId, index)).exists(_ != NotAnsweredYet)

        val shouldDisplayNext = areWeRecategorising && hasAssessmentBeenAnswered && mode == NormalMode

        val commodityCodeWithoutZeros = commodityCode.reverse.dropWhile(char => char == '0').reverse

        val shouldGoToLongerCommodityCode =
          exemptions.isEmpty && category == 2 && commodityCodeWithoutZeros.length <= 6 &&
            categorisationInfo.descendantCount != 0

        val shouldGoToCya = exemptions.isEmpty && !shouldGoToLongerCommodityCode

        (shouldGoToCya, shouldGoToLongerCommodityCode, shouldDisplayNext) match {
          case (true, _, _) =>
            Future.successful(
              Redirect(
                navigator
                  .nextPage(AssessmentPage(recordId, index, shouldRedirectToCya = true), mode, request.userAnswers)
              )
            )
          case (_, true, _) =>
            Future.successful(Redirect(routes.LongerCommodityCodeController.onPageLoad(mode, recordId).url))
          case (_, _, true) =>
            Future.successful(Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, request.userAnswers)))
          case _ => Future.successful(Ok(view(preparedForm, mode, recordId, index, listItems, commodityCode)))
        }
      }

      categorisationResult.flatMap(identity).recover { case _ =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request => {

      request.userAnswers.get(CategorisationDetailsQuery2(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
            val listItems = assessment.getExemptionListItems
            val form = formProvider2(listItems.size)

            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, mode, recordId, index, listItems, categorisationInfo.commodityCode))),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage2(recordId, index), value))
                    _ <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(AssessmentPage2(recordId, index), mode, updatedAnswers))
              )
          }}.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
        }
    }
}
