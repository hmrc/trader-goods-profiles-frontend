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

import connectors.TraderProfileConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.AssessmentFormProvider
import logging.Logging
import models.AssessmentAnswer.Exemption
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.{AssessmentAnswer, Category1, Mode, UserAnswers, ott}
import navigation.Navigator
import pages.AssessmentPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants.niphlsAssessment
import viewmodels.AssessmentViewModel
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
  categorisationService: CategorisationService,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val categorisationResult = for {
        // TODO ?? userAnswersWithCategorisations <- categorisationService.requireCategorisation(request, recordId)
        traderProfile <- traderProfileConnector.getTraderProfile(request.eori)
        recordQuery = request.userAnswers.get(RecordCategorisationsQuery)
        categorisationInfo <- Future.fromTry(Try(recordQuery.get.records(recordId)))
        listItems = categorisationInfo.categoryAssessments(index).getExemptionListItems
        commodityCode = categorisationInfo.commodityCode
        exemptions = categorisationInfo.categoryAssessments(index).exemptions
        form = formProvider(exemptions.size)
        userHasNiphls = traderProfile.niphlNumber.isDefined
        isNiphlsAnAnswer = categorisationInfo.categoryAssessments(index).isNiphlsAnswer
        updatedAnswers <-
          updateAnswerIfNiphls(recordId, index, request.userAnswers, isNiphlsAnAnswer, userHasNiphls)
        _ <- sessionRepository.set(updatedAnswers)
        preparedForm = updatedAnswers.get(AssessmentPage(recordId, index)) match {
          case Some(value) => form.fill(value)
          case None => form
        }
      } yield {
        val exemptions = categorisationInfo.categoryAssessments(index).exemptions
        val isNiphlsCategory2Assessment = categorisationInfo.categoryAssessments(index).isEmptyCat2Assessment &&
          categorisationInfo.isNiphls && userHasNiphls

        (userHasNiphls, isNiphlsAnAnswer, isNiphlsCategory2Assessment, exemptions.isEmpty) match {
          case (true, true, _, _) =>
            Future.successful(Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers)))
          case (true, false, true, _) =>
            Future.successful(
              Redirect(
                navigator.nextPage(
                  AssessmentPage(recordId, index, shouldRedirectToCya = true),
                  mode,
                  updatedAnswers
                )
              )
            )
          case (false, true, _, _) =>
            // Should have not been allowed to get this far. Should not have loaded this page
            Future.successful(
              //TODO ???
              Redirect(
                routes.JourneyRecoveryController.onPageLoad()
              )
            )
          case (false, _, _, true) =>
            Future.successful(
              Redirect(
                navigator
                  .nextPage(AssessmentPage(recordId, index, shouldRedirectToCya = true), mode, request.userAnswers)
              )
            )
          case _ =>

            displayPage(mode, recordId, index,
              updatedAnswers, categorisationInfo, exemptions)
        }

      }

      categorisationResult.flatMap(identity).recover { case _ =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def displayPage(
    mode: Mode,
    recordId: String,
    index: Int,
    userAnswersWithCategorisations: UserAnswers,
    categorisationInfo: CategorisationInfo,
    exemptions: Seq[ott.Exemption],
      listItems: Seq[String],
    commodityCode: String
  )(implicit messages: Messages,
    request: DataRequest[_]
  ) = {
    val form         = formProvider(exemptions.map(_.id))
    val preparedForm = userAnswersWithCategorisations.get(AssessmentPage(recordId, index)) match {
      case Some(value) => form.fill(value)
      case None        => form
    }

    val radioOptions = AssessmentAnswer.radioOptions(exemptions)(messages)
    val viewModel    = AssessmentViewModel(
      commodityCode = categorisationInfo.commodityCode,
      numberOfThisAssessment = index + 1,
      numberOfAssessments = categorisationInfo.categoryAssessments.size,
      radioOptions = radioOptions
    )

    Future.successful(Ok(view(preparedForm, mode, recordId, index, listItems, commodityCode)(request, messages)))
  }

  private def updateAnswerIfNiphls(
    recordId: String,
    index: Int,
    userAnswersWithCategorisations: UserAnswers,
    isNiphlsAnAnswer: Boolean,
    userHasNiphls: Boolean
  ) =
    if (isNiphlsAnAnswer && userHasNiphls)
      Future.fromTry(userAnswersWithCategorisations.set(AssessmentPage(recordId, index), Exemption(niphlsAssessment)))
    else Future.successful(userAnswersWithCategorisations)

  def onSubmit(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      {
        for {
          recordQuery        <- request.userAnswers.get(RecordCategorisationsQuery)
          categorisationInfo <- recordQuery.records.get(recordId)
          listItems           = categorisationInfo.categoryAssessments(index).getExemptionListItems
          commodityCode       = categorisationInfo.commodityCode
          exemptions          = categorisationInfo.categoryAssessments(index).exemptions
          form                = formProvider(exemptions.size)
        } yield form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, recordId, index, listItems, commodityCode))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage(recordId, index), value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers))
          )
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }
}
