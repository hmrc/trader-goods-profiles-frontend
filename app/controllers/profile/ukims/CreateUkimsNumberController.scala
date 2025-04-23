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

package controllers.profile.ukims

import controllers.BaseController
import controllers.actions.*
import controllers.profile.ukims.routes.*
import forms.profile.ukims.UkimsNumberFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.ukims.UkimsNumberPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.UkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateUkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: UkimsNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: UkimsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(
        view(
          preparedForm,
          CreateUkimsNumberController.onSubmit(mode),
          isCreateJourney = true
        )
      )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, CreateUkimsNumberController.onSubmit(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberPage, mode, updatedAnswers))
        )
    }

}
