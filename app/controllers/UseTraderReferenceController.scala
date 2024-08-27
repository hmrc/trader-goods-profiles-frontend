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
import forms.UseTraderReferenceFormProvider
import models.Mode
import navigation.Navigator
import pages.{TraderReferencePage, UseTraderReferencePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.UseTraderReferenceView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class UseTraderReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: UseTraderReferenceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: UseTraderReferenceView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(UseTraderReferencePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(TraderReferencePage) match {
        case Some(traderReference) => Ok(view(preparedForm, traderReference, mode))
        case None                  => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(TraderReferencePage) match {
              case Some(traderReference) => Future.successful(BadRequest(view(formWithErrors, traderReference, mode)))
              case None                  => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UseTraderReferencePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UseTraderReferencePage, mode, updatedAnswers))
        )
    }
}
