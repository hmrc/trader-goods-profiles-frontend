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

package controllers.profile.nirms

import controllers.BaseController
import controllers.actions.*
import controllers.profile.nirms.routes.*
import forms.profile.nirms.NirmsNumberFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.nirms.NirmsNumberPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.NirmsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateNirmsNumberController @Inject()(
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: NirmsNumberFormProvider,
  checkProfile: ProfileCheckAction,
  val controllerComponents: MessagesControllerComponents,
  view: NirmsNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(NirmsNumberPage, form)
      Ok(view(preparedForm, CreateNirmsNumberController.onSubmit(mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, CreateNirmsNumberController.onSubmit(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NirmsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NirmsNumberPage, mode, updatedAnswers))
        )
    }


}
