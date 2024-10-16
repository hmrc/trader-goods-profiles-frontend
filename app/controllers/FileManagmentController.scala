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
import models.DownloadDataStatus.{FileReadySeen, FileReadyUnseen}
import models.DownloadDataSummary
import navigation.Navigator
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.DateTimeFormats.dateTimeFormat
import views.html.FileReadyView

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileManagmentController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents,
  view: FileReadyView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(): Action[AnyContent]                               = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      (for {
        Some(downloadDataSummary) <- downloadDataConnector.getDownloadDataSummary(request.eori)
        if isFileReady(downloadDataSummary)
        Some(fileInfo)            <- Future.successful(downloadDataSummary.fileInfo)
        Some(downloadData)        <- downloadDataConnector.getDownloadData(request.eori)
      } yield Ok(
        view(
          fileInfo.fileSize,
          downloadData.downloadURL,
          convertToDateString(fileInfo.fileCreated),
          convertToDateString(
            fileInfo.fileCreated
              .plus(fileInfo.retentionDays.toInt, ChronoUnit.DAYS)
          )
        )
      )).recover { case _ => navigator.journeyRecovery() }
  }
  def isFileReady(downloadDataSummary: DownloadDataSummary): Boolean =
    downloadDataSummary.status == FileReadyUnseen || downloadDataSummary.status == FileReadySeen

  def convertToDateString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    instant.atZone(ZoneOffset.UTC).toLocalDate.format(dateTimeFormat())
  }
}
