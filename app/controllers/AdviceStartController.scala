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
import models.NormalMode
import navigation.Navigator
import pages.AdviceStartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AdviceStartView

import javax.inject.Inject

class AdviceStartController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: AdviceStartView,
  navigator: Navigator
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(view(recordId))
  }

  //TODO navigate to correct page
  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(AdviceStartPage(recordId), NormalMode, request.userAnswers))
  }
}
