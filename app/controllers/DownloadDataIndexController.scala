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
import models.DownloadDataStatus._
import models.DownloadDataSummary
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadDataIndexController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  def redirect: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      downloadDataConnector.getDownloadDataSummary(request.eori).map { route =>
        Redirect(getDownloadLinkRoute(route))
      }
  }

  private def getDownloadLinkRoute(opt: Option[Seq[DownloadDataSummary]]): Call =
    opt.map(_.head).map(_.status) match { // TODO: Double check this, are we going to preserve the old page functionallity until they are removed?
      case Some(FileInProgress)                        =>
        routes.FileInProgressController.onPageLoad()
      case Some(FileReadyUnseen) | Some(FileReadySeen) =>
        routes.FileReadyController.onPageLoad()
      case _                                           => routes.RequestDataController.onPageLoad()
    }
}
