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

import connectors.{DownloadDataConnector, GoodsRecordConnector}
import controllers.actions._
import models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen}
import models.DownloadDataSummary
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
  goodsRecordConnector: GoodsRecordConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HomePageView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getOrCreate).async { implicit request =>
    for {
      downloadDataSummary <- downloadDataConnector.getDownloadDataSummary(request.eori)
      goodsRecords        <- goodsRecordConnector.getRecords(request.eori, 1, 1)
      doesGoodsRecordExist = goodsRecords.nonEmpty
    } yield {
      val downloadLinkMessagesKey = getDownloadLinkMessagesKey(downloadDataSummary, doesGoodsRecordExist)
      downloadDataSummary match {
        case Some(downloadDataSummary) if downloadDataSummary.status == FileReadyUnseen =>
          Ok(view(downloadReady = true, downloadLinkMessagesKey))
        case _                                                                          =>
          Ok(view(downloadReady = false, downloadLinkMessagesKey))
      }
    }
  }

  private def getDownloadLinkMessagesKey(opt: Option[DownloadDataSummary], doesGoodsRecordExist: Boolean): String =
    if (doesGoodsRecordExist) {
      opt.map(_.status) match {
        case Some(FileInProgress)  =>
          "homepage.downloadLinkText.fileInProgress"
        case Some(FileReadyUnseen) =>
          "homepage.downloadLinkText.fileReady"
        case Some(FileReadySeen)   =>
          "homepage.downloadLinkText.fileReady"
        case _                     =>
          "homepage.downloadLinkText.requestFile"
      }
    } else {
      "homepage.downloadLinkText.noGoodsRecords"
    }
}
