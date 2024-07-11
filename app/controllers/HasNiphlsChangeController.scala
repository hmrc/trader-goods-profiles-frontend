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
import forms.HasNiphlsChangeFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.HasNiphlsChangePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasNiphlsChangeView

import scala.concurrent.{ExecutionContext, Future}

class HasNiphlsChangeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasNiphlsChangeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasNiphlsChangeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(HasNiphlsChangePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, NormalMode))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, NormalMode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasNiphlsChangePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasNiphlsChangePage, NormalMode, updatedAnswers))
        )
  }
}
