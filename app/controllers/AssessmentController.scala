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

import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.AssessmentFormProvider
import logging.Logging
import models.{AssessmentAnswer, CategoryRecord, Mode, Scenario, UserAnswers}
import navigation.Navigator
import pages.AssessmentPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
  goodsRecordConnector: GoodsRecordConnector,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport with Logging {

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val categorisationResult = for {
        userAnswersWithCategorisations <- categorisationService.requireCategorisation(request, recordId)
        recordQuery = userAnswersWithCategorisations.get(RecordCategorisationsQuery)
        categorisationInfo <- Future.fromTry(Try(recordQuery.get.records.get(recordId).get))
      } yield {
        val exemptions = categorisationInfo.categoryAssessments(index).exemptions
        val form = formProvider(exemptions.map(_.id))
        val preparedForm = userAnswersWithCategorisations.get(AssessmentPage(recordId, index)) match {
          case Some(value) => form.fill(value)
          case None => form
        }
        val radioOptions = AssessmentAnswer.radioOptions(exemptions)

        val viewModel = AssessmentViewModel(
          commodityCode = categorisationInfo.commodityCode,
          numberOfThisAssessment = index + 1,
          numberOfAssessments = categorisationInfo.categoryAssessments.size,
          radioOptions = radioOptions
        )

        if (exemptions.isEmpty) {
          handleNoExemptions(userAnswersWithCategorisations, recordId, request.eori)
        } else {
          Future.successful(Ok(view(preparedForm, mode, recordId, index, viewModel)))
        }
      }

      categorisationResult.flatMap(identity).recover { case _ =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def handleNoExemptions(userAnswers: UserAnswers, recordId: String, eori: String)(implicit request: Request[_]): Future[Result] = {
    CategoryRecord
      .build(userAnswers, eori, recordId)
      .map { categoryRecord =>
        goodsRecordConnector
          .updateCategoryForGoodsRecord(eori, recordId, categoryRecord)
          .map { _ =>
            Redirect(routes.CyaCategorisationController.onPageLoad(recordId).url)
          }
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))
  }


  def onSubmit(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      {
        for {
          recordQuery        <- request.userAnswers.get(RecordCategorisationsQuery)
          categorisationInfo <- recordQuery.records.get(recordId)
        } yield {

          val exemptions = categorisationInfo.categoryAssessments(index).exemptions
          val form       = formProvider(exemptions.map(_.id))

          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val radioOptions = AssessmentAnswer.radioOptions(exemptions)
                val viewModel    = AssessmentViewModel(
                  commodityCode = categorisationInfo.commodityCode,
                  numberOfThisAssessment = index + 1,
                  numberOfAssessments = categorisationInfo.categoryAssessments.size,
                  radioOptions = radioOptions
                )

                Future.successful(BadRequest(view(formWithErrors, mode, recordId, index, viewModel)))
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage(recordId, index), value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers))
            )
        }
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }
}
