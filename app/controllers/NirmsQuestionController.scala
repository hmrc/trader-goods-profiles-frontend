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
import forms.NirmsQuestionFormProvider
import models.TraderGoodsProfile
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NirmsQuestionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NirmsQuestionController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  view: NirmsQuestionView,
  formProvider: NirmsQuestionFormProvider,
  sessionRequest: SessionRequestAction,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen sessionRequest) { implicit request =>
    val optionalHasNirms = request.userAnswers.traderGoodsProfile.hasNirms

    optionalHasNirms match {
      case Some(hasNirmsAnswer) => Ok(view(form.fill(hasNirmsAnswer)))
      case None                 => Ok(view(form))
    }
  }

  def onSubmit: Action[AnyContent] = (authorise andThen sessionRequest).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        hasNirmsAnswer => {
          val updatedTgpModelObject = request.userAnswers.traderGoodsProfile.copy(hasNirms = Some(hasNirmsAnswer))

          val updatedUserAnswers = request.userAnswers.copy(traderGoodsProfile = updatedTgpModelObject)

          sessionService
            .updateUserAnswers(updatedUserAnswers)
            .fold(
              sessionError => Redirect(routes.JourneyRecoveryController.onPageLoad().url),
              success =>
                if (hasNirmsAnswer) {
                  Redirect(routes.NirmsNumberController.onPageLoad.url)
                } else {
                  Redirect(routes.NiphlQuestionController.onPageLoad.url)
                }
            )
        }
      )
  }

}
