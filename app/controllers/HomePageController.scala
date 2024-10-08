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

import connectors.DownloadDataConnector
import controllers.actions._
import models.DownloadDataStatus.FileReadyUnseen
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.HomePageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HomePageController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getOrCreate: DataRetrievalOrCreateAction,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HomePageView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getOrCreate).async { implicit request =>
    downloadDataConnector.getDownloadDataSummary(request.eori).map {
      case Some(downloadDataSummary) if downloadDataSummary.status == FileReadyUnseen => Ok(view(downloadReady = true))
      case _                                                                          => Ok(view(downloadReady = false))
    }
  }
}
