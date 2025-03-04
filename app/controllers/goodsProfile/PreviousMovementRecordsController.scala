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

package controllers.goodsProfile

import controllers.BaseController
import controllers.actions._
import models.NormalMode
import navigation.GoodsProfileNavigator
import pages.goodsProfile.PreviousMovementRecordsPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.goodsProfile.PreviousMovementRecordsView

import javax.inject.Inject

class PreviousMovementRecordsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  profileAuth: ProfileAuthenticateAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: PreviousMovementRecordsView,
  navigator: GoodsProfileNavigator
) extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth) { implicit request =>
    Ok(view())
  }

  def onSubmit: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(PreviousMovementRecordsPage, NormalMode, request.userAnswers))
  }
}
