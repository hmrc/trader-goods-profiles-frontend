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

package models.download

import helpers.FileManagementTableComponentHelper
import models.{DownloadData, DownloadDataSummary}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.DateTimeFormats

trait FileManagementTable {
  val caption: String
  val body: Option[String]
  val headRows: Seq[HeadCell]
  val rows: Seq[Seq[TableRow]]
}

case class AvailableFilesTable(availableFileRows: Seq[Seq[TableRow]])(implicit messages: Messages)
    extends FileManagementTable {
  override val caption: String          = messages("fileManagement.availableFiles.table.caption")
  override val body: Option[String]     = None
  override val headRows: Seq[HeadCell]  =
    Seq(
      messages("fileManagement.availableFiles.table.header1"),
      messages("fileManagement.availableFiles.table.header2"),
      messages("fileManagement.availableFiles.table.header3")
    ).map { text =>
      HeadCell(content = Text(text))
    }
  override val rows: Seq[Seq[TableRow]] = availableFileRows
}

object AvailableFilesTable {
  def apply(
    availableFiles: Option[Seq[(DownloadDataSummary, DownloadData)]]
  )(implicit
    messages: Messages,
    fileManagementTableComponentHelper: FileManagementTableComponentHelper
  ): Option[AvailableFilesTable] = {

    val availableFileRows = availableFiles.map {
      _.map { availableFile =>
        val (summary, data) = availableFile

        val fileCreated        = DateTimeFormats.convertToDateTimeString(summary.createdAt)
        val fileExpirationDate = DateTimeFormats.convertToDateTimeString(summary.expiresAt)
        val fileLink           = fileManagementTableComponentHelper.createLink(
          messages("fileManagement.availableFiles.downloadText"),
          messages("fileManagement.availableFiles.downloadText.hidden", fileCreated),
          data.downloadURL,
          data.fileSize.toString

        )

        Seq(
          TableRow(
            content = Text(fileCreated)
          ),
          TableRow(
            content = Text(fileExpirationDate)
          ),
          TableRow(
            content = fileLink
          )
        )
      }
    }

    availableFileRows.map {
      new AvailableFilesTable(_)
    }
  }
}

object PendingFilesTable {
  def apply(
    pendingFiles: Option[Seq[DownloadDataSummary]]
  )(implicit
    messages: Messages,
    fileManagementTableComponentHelper: FileManagementTableComponentHelper
  ): Option[PendingFilesTable] = {

    val pendingFilesRows = pendingFiles.map {
      _.map { pendingFile =>
        val fileCreated = DateTimeFormats.convertToDateTimeString(pendingFile.createdAt)
        val fileLink    = fileManagementTableComponentHelper.createTag(messages("fileManagement.pendingFiles.fileText"))

        Seq(
          TableRow(
            content = Text(fileCreated)
          ),
          TableRow(
            content = fileLink
          )
        )
      }
    }

    pendingFilesRows.map { tableRows =>
      new PendingFilesTable(tableRows)
    }
  }
}

case class PendingFilesTable(pendingFileRows: Seq[Seq[TableRow]])(implicit messages: Messages)
    extends FileManagementTable {
  override val caption: String          = messages("fileManagement.pendingFiles.table.caption")
  override val body: Option[String]     = Some(messages("fileManagement.pendingFiles.table.body"))
  override val headRows: Seq[HeadCell]  =
    Seq(
      messages("fileManagement.pendingFiles.table.header1"),
      messages("fileManagement.pendingFiles.table.header2")
    ).map { content =>
      HeadCell(content = Text(content))
    }
  override val rows: Seq[Seq[TableRow]] = pendingFileRows
}
