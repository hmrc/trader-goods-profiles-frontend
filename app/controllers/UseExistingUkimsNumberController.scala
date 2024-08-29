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
import forms.UseExistingUkimsFormProvider
import models.NormalMode
import navigation.Navigator
import pages.{UkimsNumberPage, UseExistingUkimsPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.UseExistingUkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UseExistingUkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: UseExistingUkimsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: UseExistingUkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      (for {
        ukimsNumber <- request.userAnswers.get(UkimsNumberPage)
        updatedForm  = request.userAnswers.get(UseExistingUkimsPage).map(value => form.fill(value)).getOrElse(form)
        updatedView  = view(updatedForm, routes.UseExistingUkimsNumberController.onSubmit(), ukimsNumber)
      } yield Ok(updatedView))
        .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful {
            request.userAnswers
              .get(UkimsNumberPage)
              .map(ukims => BadRequest(view(formWithErrors, routes.UseExistingUkimsNumberController.onSubmit(), ukims)))
              .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          },
        useExistingUkims =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UseExistingUkimsPage, useExistingUkims))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UseExistingUkimsPage, NormalMode, updatedAnswers))
      )
  }

}
