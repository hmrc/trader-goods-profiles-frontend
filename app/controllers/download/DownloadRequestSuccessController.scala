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

package controllers.download

import connectors.DownloadDataConnector
import controllers.actions._
import controllers.{BaseController, routes}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.download.DownloadRequestSuccessView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadRequestSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents,
  view: DownloadRequestSuccessView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      downloadDataConnector.getEmail(request.eori).map {
        case Some(email) => Ok(view(email.address))
        case _           =>
          logErrorsAndContinue(
            "Email was not found",
            routes.IndexController.onPageLoad()
          )
      }
  }
}