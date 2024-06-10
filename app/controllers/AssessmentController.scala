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
import forms.AssessmentFormProvider
import models.{AssessmentAnswer, Mode}
import navigation.Navigator
import pages.AssessmentPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{RecordCategorisationsQuery}
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.AssessmentViewModel
import views.html.AssessmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      categorisationService
        .requireCategorisation(request, recordId)
        .flatMap[Result] { _ =>
          val optionalResult = for {
            recordQuery        <- request.userAnswers.get(RecordCategorisationsQuery)
            categorisationInfo <- recordQuery.records.get(recordId)
          } yield {
            val exemptions   = categorisationInfo.categoryAssessments(index).exemptions
            val form         = formProvider(exemptions.map(_.id))
            val preparedForm = request.userAnswers.get(AssessmentPage(recordId, index)) match {
              case Some(value) => form.fill(value)
              case None        => form
            }

            val radioOptions = AssessmentAnswer.radioOptions(exemptions)
            val viewModel    = AssessmentViewModel(
              commodityCode = categorisationInfo.commodityCode,
              numberOfThisAssessment = index + 1,
              numberOfAssessments = categorisationInfo.categoryAssessments.size,
              radioOptions = radioOptions
            )

            Ok(view(preparedForm, mode, recordId, index, viewModel))
          }

          Future.successful(optionalResult.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad())))
        }
        .recoverWith { case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
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
