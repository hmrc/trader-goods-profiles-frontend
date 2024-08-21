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
import pages.ProfileSetupPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.ProfileSetupView

import javax.inject.Inject

class ProfileSetupController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: ProfileSetupView,
  navigator: Navigator,
  requireData: DataRequiredAction,
  getOrCreate: DataRetrievalOrCreateAction,
  checkProfile: ProfileCheckAction
) extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen checkProfile andThen getOrCreate) { implicit request =>
    Ok(view())
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(ProfileSetupPage, NormalMode, request.userAnswers))

  }
}
