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

import controllers.actions.{AuthoriseAction, SessionRequestAction}
import forms.NiphlNumberFormProvider
import models.NiphlNumber
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NiphlNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NiphlNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  view: NiphlNumberView,
  formProvider: NiphlNumberFormProvider,
  sessionRequest: SessionRequestAction,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen sessionRequest) { implicit request =>
    val optionalNiphlNumber = request.userAnswers.traderGoodsProfile.niphlNumber

    optionalNiphlNumber match {
      case Some(niphlNumber) => Ok(view(form.fill(niphlNumber.value)))
      case None              => Ok(view(form))
    }
  }

  def onSubmit: Action[AnyContent] = (authorise andThen sessionRequest).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        niphlNumber => {
          val updatedTgpModelObject =
            request.userAnswers.traderGoodsProfile
              .copy(hasNiphl = Some(true), niphlNumber = Some(NiphlNumber(niphlNumber)))
          val updatedUserAnswers    = request.userAnswers.copy(traderGoodsProfile = updatedTgpModelObject)

          sessionService
            .updateUserAnswers(updatedUserAnswers)
            .fold(
              sessionError => Redirect(routes.JourneyRecoveryController.onPageLoad().url),
              success => Redirect(routes.CheckYourAnswersController.onPageLoad.url)
            )
        }
      )
  }
}
