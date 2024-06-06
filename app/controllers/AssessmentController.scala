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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CategorisationQuery
import repositories.SessionRepository
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
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode, assessmentId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      {
        for {
          categorisationInfo <- request.userAnswers.get(CategorisationQuery)
          assessment         <- categorisationInfo.categoryAssessments.find(_.id == assessmentId)
          assessmentIndex     = categorisationInfo.categoryAssessments.indexOf(assessment)
        } yield {

          val form = formProvider(assessment.exemptions.map(_.id))

          val preparedForm = request.userAnswers.get(AssessmentPage(assessmentId)) match {
            case Some(value) => form.fill(value)
            case None        => form
          }

          val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)
          val viewModel    = AssessmentViewModel(
            commodityCode = categorisationInfo.commodityCode,
            numberOfThisAssessment = assessmentIndex + 1,
            numberOfAssessments = categorisationInfo.categoryAssessments.size,
            radioOptions = radioOptions
          )

          Ok(view(preparedForm, mode, assessmentId, viewModel))
        }
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(mode: Mode, assessmentId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      {
        for {
          categorisationInfo <- request.userAnswers.get(CategorisationQuery)
          assessment         <- categorisationInfo.categoryAssessments.find(_.id == assessmentId)
          assessmentIndex     = categorisationInfo.categoryAssessments.indexOf(assessment)
        } yield {

          val form = formProvider(assessment.exemptions.map(_.id))

          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)
                val viewModel    = AssessmentViewModel(
                  commodityCode = categorisationInfo.commodityCode,
                  numberOfThisAssessment = assessmentIndex + 1,
                  numberOfAssessments = categorisationInfo.categoryAssessments.size,
                  radioOptions = radioOptions
                )

                Future.successful(BadRequest(view(formWithErrors, mode, assessmentId, viewModel)))
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage(assessmentId), value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(AssessmentPage(assessmentId), mode, updatedAnswers))
            )
        }
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }
}