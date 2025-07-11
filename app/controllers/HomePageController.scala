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

import connectors.{DownloadDataConnector, GoodsRecordConnector, TraderProfileConnector}
import controllers.actions.*
import models.DownloadDataStatus.FileReadyUnseen
import models.GoodsRecordsPagination.firstPage
import models.download.DownloadLinkText
import models.{DownloadDataSummary, HistoricProfileData}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.SessionData.{newUkimsNumberPage, pageUpdated}
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
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: HomePageView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getOrCreate).async { implicit request =>
    for {
      downloadDataSummary <- downloadDataConnector.getDownloadDataSummary
      verifiedEmail       <- downloadDataConnector.getEmail.map {
                               case Some(_) => true
                               case None    => false
                             }
      goodsRecords        <- goodsRecordConnector.getRecords(1, 1)
      doesGoodsRecordExist = goodsRecords.exists(_.goodsItemRecords.nonEmpty)
      historicProfileData <- traderProfileConnector.getHistoricProfileData(request.eori)
    } yield
      if (goodsRecords.isEmpty) {
        Redirect(
          controllers.goodsProfile.routes.GoodsRecordsLoadingController
            .onPageLoad(Some(RedirectUrl(controllers.routes.HomePageController.onPageLoad().url)))
        )
      } else {
        val downloadLinkText            = DownloadLinkText(downloadDataSummary, doesGoodsRecordExist, verifiedEmail)
        val showNewUkimsBanner: Boolean = request.session.get(pageUpdated).contains(newUkimsNumberPage)
        val viewUpdateGoodsRecordsLink  = getViewUpdateRecordsLink(historicProfileData)

        Ok(
          view(
            downloadReady = downloadReady(downloadDataSummary),
            downloadLinkText = downloadLinkText,
            ukimsNumberChanged = showNewUkimsBanner,
            doesGoodsRecordExist = doesGoodsRecordExist,
            eoriNumber = request.eori,
            viewUpdateGoodsRecordsLink = viewUpdateGoodsRecordsLink
          )
        )
          .removingFromSession(pageUpdated)
      }
  }

  private def downloadReady(downloadDataSummary: Seq[DownloadDataSummary]): Boolean =
    downloadDataSummary
      .collectFirst {
        case summary if summary.status == FileReadyUnseen => true
      }
      .getOrElse(false)

  private def getViewUpdateRecordsLink(historicProfileData: Option[HistoricProfileData]): String =
    historicProfileData match {
      case Some(_) => controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
      case _       => controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
    }
}
