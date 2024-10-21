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
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Tag, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content
import utils.DateTimeFormats.dateTimeFormat

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset}

trait FileRow {
  val fileCreated: String
  val fileExpirationDate: Option[String]
  val fileLink: Content
}

case class AvailableFileRow(fileCreated: String, fileExpirationDate: Option[String], fileLink: Content) extends FileRow
case class PendingFileRow(fileCreated: String, fileExpirationDate: Option[String], fileLink: Content) extends FileRow

object FileRow {

  private def convertToDateString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    instant.atZone(ZoneOffset.UTC).toLocalDate.format(dateTimeFormat())
  }

  private def retentionTimeToExpirationDate(fileInfo: FileInfo)(implicit messages: Messages): String = convertToDateString(fileInfo.fileCreated
      .plus(fileInfo.retentionDays.toInt, ChronoUnit.DAYS))

  object AvailableFileRow {
    def apply(rowData: (DownloadDataSummary, DownloadData))(implicit messages: Messages): Option[AvailableFileRow] = {
      val (summary, data) = rowData

      summary.fileInfo.map {fileInfo =>
        val fileCreated = convertToDateString(fileInfo.fileCreated) // Double check that this is actually the date and time of the request
        val fileExpirationDate = retentionTimeToExpirationDate(fileInfo)
        val fileLink = HtmlContent(
          s"""<a href="${data.downloadURL}" class="govuk-link">${messages("fileManagement.availableFiles.downloadText")}</a>"""
        )

        new AvailableFileRow(fileCreated, Some(fileExpirationDate), fileLink)
      }
    }
  }

  object PendingFileRow { // TODO: Do we want to use these then convert to table row? Or just use the table row directly?
    def apply(summaryData: DownloadDataSummary)(implicit messages: Messages): Option[PendingFileRow] = {
      summaryData.fileInfo.map {fileInfo =>
        val fileCreated = convertToDateString(fileInfo.fileCreated) // Double check that this is actually the date and time of the request
        val fileLink = Tag(
            content = Text(messages("fileManagement.pendingFiles.fileText"))
          ).content

        new PendingFileRow(fileCreated, None, fileLink)
      }
    }
  }
}