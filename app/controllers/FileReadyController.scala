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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.FileReadyView

import javax.inject.Inject

class FileReadyController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: FileReadyView
) extends BaseController {

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      // TODO: Get file size to pass in to view
      // TODO: Get file download link to pass in to view
      // TODO: Get file created date and available until date
      val fileSizeKilobytes = 1024
      val fileDownloadLink  = "www.example.com"
      val createdDate       = "19 July 2024"
      val availableUntil    = "18 August 2024"
      Ok(view(fileSizeKilobytes, fileDownloadLink, createdDate, availableUntil))
  }

}
