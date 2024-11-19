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

package controllers.newUkims

import controllers.actions._
import models.NormalMode
import navigation.NewUkimsNavigator
import pages.newUkims.{NewUkimsNumberPage, UkimsNumberChangePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.newUkims.UkimsNumberChangeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkimsNumberChangeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getOrCreate: DataRetrievalOrCreateAction,
  val controllerComponents: MessagesControllerComponents,
  view: UkimsNumberChangeView,
  navigator: NewUkimsNavigator,
  profileAuth: ProfileAuthenticateAction,
  checkEori: EoriCheckAction,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen checkEori andThen getOrCreate) {
    implicit request =>
      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = (identify andThen profileAuth andThen checkEori andThen getOrCreate).async {
    implicit request =>
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.remove(NewUkimsNumberPage))

        _ <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(UkimsNumberChangePage, NormalMode, updatedAnswers))
  }
}
