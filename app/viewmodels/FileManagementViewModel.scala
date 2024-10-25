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

package viewmodels

import connectors.DownloadDataConnector
import helpers.FileManagementTableComponentHelper
import models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen}
import models.filemanagement._
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class FileManagementViewModel(
  availableFilesTable: Option[AvailableFilesTable],
  pendingFilesTable: Option[PendingFilesTable]
)(implicit messages: Messages) {

  val isFiles: Boolean = availableFilesTable.isDefined || pendingFilesTable.isDefined

  val title: String   = messages("fileManagement.title")
  val heading: String = messages("fileManagement.heading")

  val paragraph1: String =
    if (isFiles) messages("fileManagement.files.paragraph1") else messages("fileManagement.noFiles.paragraph1")

  val tgpRecordsLink: String =
    if (isFiles) {
      messages("fileManagement.files.requestRecord.linkText")
    } else {
      messages("fileManagement.noFiles.requestRecord.linkText")
    }

  val goBackHomeLink: String = messages("site.goBackToHomePage")
}

object FileManagementViewModel {
  class FileManagementViewModelProvider @Inject() (implicit
    fileManagementTableComponentHelper: FileManagementTableComponentHelper
  ) {
    def apply(
      eori: String,
      downloadDataConnector: DownloadDataConnector
    )(implicit messages: Messages, ec: ExecutionContext, hc: HeaderCarrier): Future[FileManagementViewModel] =
      for {
        downloadDataSummary <- downloadDataConnector.getDownloadDataSummary(eori)
        downloadData        <- downloadDataConnector.getDownloadData(eori)

        availableDataSummaries = downloadDataSummary.filterToOption(summary =>
                                   summary.status == FileReadySeen || summary.status == FileReadyUnseen
                                 )

        availableFiles = availableDataSummaries.flatMap { availableFilesSeq =>
                           val files = for {
                             availableFile    <- availableFilesSeq
                             fileInfo         <- availableFile.fileInfo
                             matchingDownload <- downloadData.find(_.filename == fileInfo.fileName)
                           } yield (availableFile, matchingDownload)

                           if (files.nonEmpty) Some(files) else None
                         }

        pendingFiles = downloadDataSummary.filterToOption(_.status == FileInProgress)

      } yield {

        if (availableFiles.isDefined) {
          downloadDataConnector.updateSeenStatus(eori)
        }

        val availableFilesTable = AvailableFilesTable(availableFiles)
        val pendingFilesTable   = PendingFilesTable(pendingFiles)

        new FileManagementViewModel(availableFilesTable, pendingFilesTable)
      }
  }
}
