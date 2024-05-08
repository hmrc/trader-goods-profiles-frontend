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

import controllers.actions._
import forms.NirmsNumberFormProvider
import models.{CheckMode, Mode, NirmsNumber, NormalMode}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NirmsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NirmsNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  view: NirmsNumberView,
  formProvider: NirmsNumberFormProvider,
  sessionRequest: SessionRequestAction,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {
  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen sessionRequest) { implicit request =>
    val optionalNirmsNumber = request.userAnswers.maintainProfileAnswers.nirmsNumber

    optionalNirmsNumber match {
      case Some(nirmsNumber) => Ok(view(form.fill(nirmsNumber.value), mode))
      case None              => Ok(view(form, mode))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen sessionRequest).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        nirmsNumber => {
          val updatedMaintainProfileAnswers =
            request.userAnswers.maintainProfileAnswers
              .copy(nirmsNumber = Some(NirmsNumber(nirmsNumber)))
          val updatedUserAnswers            = request.userAnswers.copy(maintainProfileAnswers = updatedMaintainProfileAnswers)

          sessionService
            .updateUserAnswers(updatedUserAnswers)
            .fold(
              sessionError => Redirect(routes.JourneyRecoveryController.onPageLoad().url),
              success => navigate(mode)
            )
        }
      )
  }

  private def navigate(mode: Mode) = mode match {
    case NormalMode => Redirect(routes.NiphlQuestionController.onPageLoad(mode).url)
    case CheckMode  => Redirect(routes.CheckYourAnswersController.onPageLoad.url)
  }
}
