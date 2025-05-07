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

package controllers.goodsRecord

import controllers.BaseController
import controllers.actions.*
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.goodsRecord.CreateRecordAutoCategorisationSuccessView

import javax.inject.Inject
import scala.concurrent.Future

class CreateRecordAutoCategorisationSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CreateRecordAutoCategorisationSuccessView
) extends BaseController {

  def onPageLoad(recordId: String, isImmiReady: Boolean): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      if (isImmiReady) {
        Future.successful(Ok(view(recordId, true)))
      } else {
        Future.successful(Ok(view(recordId, false)))
      }
    }
}
