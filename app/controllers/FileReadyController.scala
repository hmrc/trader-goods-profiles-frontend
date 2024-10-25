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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.DateTimeFormats.convertToDateString
import views.html.FileReadyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileReadyController @Inject() (
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

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      val result = for {
        downloadDataSummaries <-
          downloadDataConnector
            .getDownloadDataSummary(request.eori)
        downloadDataSummary    = downloadDataSummaries.maxBy(_.createdAt)
        if isFileReady(downloadDataSummary)
        fileInfo              <- Future.successful(downloadDataSummary.fileInfo)
        downloadDatas         <-
          downloadDataConnector
            .getDownloadData(request.eori)
        downloadData           = fileInfo.flatMap(fileInfo => downloadDatas.find(_.filename == fileInfo.fileName))

      } yield downloadData.map { downloadData =>
        Ok(
          view(
            downloadData.fileSize,
            downloadData.downloadURL,
            convertToDateString(downloadDataSummary.createdAt),
            convertToDateString(
              downloadDataSummary.expiresAt
            )
          )
        )
      }

      result
        .map { result =>
          result.getOrElse(navigator.journeyRecovery())
        }
        .recover { case _ =>
          navigator.journeyRecovery()
        }
  }

  private def isFileReady(downloadDataSummary: DownloadDataSummary): Boolean =
    downloadDataSummary.status == FileReadyUnseen || downloadDataSummary.status == FileReadySeen
}
