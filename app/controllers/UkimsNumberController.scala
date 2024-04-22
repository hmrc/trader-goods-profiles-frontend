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
import forms.UkimsNumberFormProvider
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.UkimsNumberPage
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkimsNumberController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 sessionRepository: SessionRepository,
                                 navigator: Navigator,
                                 getData: DataRetrievalAction,
                                 requireData: DataRequiredAction,
                                 identify: IdentifierAction,
                                 view: UkimsNumberView,
                                 formProvider: UkimsNumberFormProvider,
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) {
    implicit request =>

//      if (request.userAnswers.isEmpty)
//        sessionRepository.set(UserAnswers("test", Json.obj()))
//      else
//        Future.unit
//      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
//        case None        => form
//        case Some(value) => form.fill(value)
//      }
      Ok(view(form, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          _ => Future.successful(Redirect(routes.DummyController.onPageLoad.url)) // TO DO
        )
    }
}
