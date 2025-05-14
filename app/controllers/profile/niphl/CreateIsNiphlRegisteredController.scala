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

package controllers.profile.niphl

import controllers.BaseController
import controllers.actions.*
import controllers.profile.niphl.routes.*
import forms.profile.niphl.HasNiphlFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.niphl.HasNiphlPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.HasNiphlView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateIsNiphlRegisteredController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: HasNiphlFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasNiphlView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasNiphlPage, form)
      Ok(
        view(
          preparedForm,
          controllers.profile.niphl.routes.CreateIsNiphlRegisteredController.onSubmit(mode),
          mode,
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
              BadRequest(
                view(
                  formWithErrors,
                  CreateIsNiphlRegisteredController.onSubmit(mode),
                  mode,
                  isCreateJourney = true
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNiphlPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNiphlPage, mode, updatedAnswers))
        )
    }

}
