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

class HomePageController @Inject()(
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
      goodsRecords <- goodsRecordConnector.getRecords(request.eori, 1, 1)
      doesGoodsRecordExist = goodsRecords.exists(_.goodsItemRecords.nonEmpty)
    } yield {
      val downloadLinkMessagesKey = getDownloadLinkMessagesKey(downloadDataSummary, doesGoodsRecordExist)
      Ok(view(downloadReady(downloadDataSummary), downloadLinkMessagesKey))
    }
  }

  private def downloadReady(downloadDataSummary: Option[Seq[DownloadDataSummary]]): Boolean =
    // TODO: Check what the expected logic is for showing the download banner,
    //  do we want to show it if any of the downloaded files are unseen, or only if the latest request is unseen?
    downloadDataSummary
      .flatMap(
        _.collectFirst {
          case summary if summary.status == FileReadyUnseen => true
        }
      )
      .getOrElse(false)

  private def getDownloadLinkMessagesKey(opt: Option[Seq[DownloadDataSummary]], doesGoodsRecordExist: Boolean): String = {
    if (doesGoodsRecordExist) {
      opt.flatMap(
        _.collectFirst {
          case summary if Seq(FileInProgress, FileReadyUnseen, FileReadySeen).contains(summary.status) =>
            "homepage.downloadLinkText.filesRequested"
        }
      ).getOrElse("homepage.downloadLinkText.noFilesRequested")
    } else {
      "homepage.downloadLinkText.noGoodsRecords"
    }
  }
}
