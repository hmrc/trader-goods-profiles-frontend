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

package controllers.profile

import controllers.BaseController
import controllers.actions._
import forms.UseExistingUkimsNumberFormProvider
import models.NormalMode
import navigation.ProfileNavigator
import pages.profile.{UkimsNumberPage, UseExistingUkimsNumberPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.UseExistingUkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UseExistingUkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: UseExistingUkimsNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: UseExistingUkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      (for {
        ukimsNumber <- request.userAnswers.get(UkimsNumberPage)
        updatedForm  = request.userAnswers.get(UseExistingUkimsNumberPage).map(value => form.fill(value)).getOrElse(form)
        updatedView  = view(updatedForm, controllers.profile.routes.UseExistingUkimsNumberController.onSubmit(), ukimsNumber)
      } yield Ok(updatedView))
        .getOrElse(navigator.journeyRecovery())
    }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful {
            request.userAnswers
              .get(UkimsNumberPage)
              .map(ukimsNumber =>
                BadRequest(view(formWithErrors, controllers.profile.routes.UseExistingUkimsNumberController.onSubmit(), ukimsNumber))
              )
              .getOrElse(navigator.journeyRecovery())
          },
        useExistingUkimsNumber =>
          for {
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(UseExistingUkimsNumberPage, useExistingUkimsNumber))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UseExistingUkimsNumberPage, NormalMode, updatedAnswers))
      )
  }

}
