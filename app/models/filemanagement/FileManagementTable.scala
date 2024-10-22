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

package models.filemanagement

import models.{DownloadData, DownloadDataSummary, FileInfo}
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.DateTimeFormats.dateTimeFormat

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset}

trait FileManagementTable {
  val caption: String
  val body: Option[String]
  val headRows: Seq[HeadCell]
  val rows: Seq[Seq[TableRow]]
}

case class AvailableFilesTable(availableFileRows: Seq[Seq[TableRow]])(implicit messages: Messages) extends FileManagementTable {
  override val caption: String = messages("fileManagement.availableFiles.table.caption")
  override val body: Option[String] = None
  override val headRows: Seq[HeadCell] =
    Seq(
      messages("fileManagement.availableFiles.table.header1"),
      messages("fileManagement.availableFiles.table.header2"),
      messages("fileManagement.availableFiles.table.header3")
    ).map { text =>
      HeadCell(content = Text(text))
    }
  override val rows: Seq[Seq[TableRow]] = availableFileRows
}

object FileManagementTable {
  private def convertToDateString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    instant.atZone(ZoneOffset.UTC).toLocalDateTime.format(dateTimeFormat())
  }

  private def retentionTimeToExpirationDate(fileInfo: FileInfo)(implicit messages: Messages): String = convertToDateString(fileInfo.fileCreated
    .plus(fileInfo.retentionDays.toInt, ChronoUnit.DAYS))

  object AvailableFilesTable {
    def apply(
               availableFiles: Option[Seq[(DownloadDataSummary, DownloadData)]]
             )(implicit messages: Messages): Option[AvailableFilesTable] = {

      val availableFileRows = availableFiles.map {
        _.flatMap { availableFile =>

          val (summary, data) = availableFile

          summary.fileInfo.map { fileInfo =>
            val fileCreated = convertToDateString(summary.createdAt)
            val fileExpirationDate = retentionTimeToExpirationDate(fileInfo)
            val fileLink = HtmlContent(
              s"""<a href="${data.downloadURL}" class="govuk-link">${messages("fileManagement.availableFiles.downloadText")}</a>"""
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
      }

      availableFileRows.map {
        new AvailableFilesTable(_)
      }
    }
  }

  object PendingFilesTable {
    def apply(
               pendingFiles: Option[Seq[DownloadDataSummary]]
             )(implicit messages: Messages): Option[PendingFilesTable] = {

      val pendingFilesRows = pendingFiles.map {
        _.flatMap { pendingFile =>

            val fileCreated = convertToDateString(pendingFile.createdAt)
            val fileLink =
              HtmlContent(s"<strong class=govuk-tag>${messages("fileManagement.pendingFiles.fileText")}</strong>")
          // TODO: Is it possible to use the component here?

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
          new PendingFilesTable(Seq(tableRows))
        }
    }
  }
}


case class PendingFilesTable(pendingFileRows: Seq[Seq[TableRow]])(implicit messages: Messages) extends FileManagementTable {
  override val caption: String = messages("fileManagement.pendingFiles.table.caption")
  override val body: Option[String] = Some(messages("fileManagement.pendingFiles.table.body"))
  override val headRows: Seq[HeadCell] =
    Seq(
      messages("fileManagement.pendingFiles.table.header1"),
      messages("fileManagement.pendingFiles.table.header2")
    ).map { content =>
      HeadCell(content = Text(content))
    }
  override val rows: Seq[Seq[TableRow]] = pendingFileRows
}
