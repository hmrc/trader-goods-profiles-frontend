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

package models

import base.SpecBase
import generators.Generators
import models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import models.filemanagement._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow

import java.time.Instant

class FileManagementTableSpec extends SpecBase with Generators {

  "FileManagementTable" - {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    implicit val message: Messages = messages(application)

    val tableRows = arbitrarySeqTableRows.sample.value

    "convertToDateString" - {
      "must return correct date string" in {
        val instant = Instant.parse("2024-04-22T10:05:00Z")
        FileManagementTable.convertToDateString(instant) mustEqual "22 April 2024 10:05"
      }
    }

    "retentionTimeToExpirationDate" - {
      "must return correct expiration date" in {
        val instant  = Instant.parse("2024-04-22T10:05:00Z")
        val fileInfo = FileInfo("url.csv", 1, instant, "30")
        FileManagementTable.retentionTimeToExpirationDate(fileInfo) mustEqual "22 May 2024 10:05"
      }
    }

    "AvailableFilesTable" - {
      val table = AvailableFilesTable(tableRows)

      "must return correct caption" in {
        table.caption mustEqual "TGP records files"
      }

      "must return correct body" in {
        table.body mustBe None
      }

      "must return correct headRows" in {
        table.headRows mustBe Seq(
          HeadCell(content = Text("Date and time requested")),
          HeadCell(content = Text("Expiry date")),
          HeadCell(content = Text("File"))
        )
      }

      ".apply" - {
        "must return convert Option[Seq[(DownloadDataSummary, DownloadData)]] into correct table rows" - {
          "when None" in {
            FileManagementTable.AvailableFilesTable(None) mustBe None
          }

          "when populated with both DownloadDataSummary and DownloadData" in {

            val instant = Instant.parse("2024-04-22T10:05:00Z")

            val fileName            = "url.csv"
            val fileInfo            = FileInfo(fileName, 1, instant, "30")
            val downloadDataSummary = DownloadDataSummary("testEori", FileReadyUnseen, instant, Some(fileInfo))
            val downloadData        = DownloadData(fileName, fileName, 1, Seq.empty)

            val tableParameter = Some(Seq((downloadDataSummary, downloadData)))

            val dateTime   = "22 April 2024 10:05"
            val expiryDate = "22 May 2024 10:05"
            val file       = s"""<a href="url.csv" class="govuk-link">Download file</a>"""

            val tableRows = Seq(
              Seq(
                TableRow(
                  content = Text(dateTime)
                ),
                TableRow(
                  content = Text(expiryDate)
                ),
                TableRow(
                  content = HtmlContent(file)
                )
              )
            )

            FileManagementTable.AvailableFilesTable(tableParameter).value.availableFileRows mustBe tableRows
          }
        }
      }
    }

    "PendingFilesTable" - {
      val table = PendingFilesTable(tableRows)

      "must return correct caption" in {
        table.caption mustEqual "Pending files"
      }

      "must return correct body" in {
        table.body mustBe Some(
          "These are requests you've made but the file is not ready yet. We'll email you when the file is ready."
        )
      }

      "must return correct headRows" in {
        table.headRows mustBe Seq(
          HeadCell(content = Text("Date and time requested")),
          HeadCell(content = Text("File"))
        )
      }

      ".apply" - {
        "must return convert Option[Seq[(DownloadDataSummary]] into correct table rows" - {
          "when None" in {
            FileManagementTable.PendingFilesTable(None) mustBe None
          }

          "when populated with both DownloadDataSummary" in {

            val instant = Instant.parse("2024-04-22T10:05:00Z")

            val downloadDataSummary = DownloadDataSummary("testEori", FileInProgress, instant, None)

            val tableParameter = Some(Seq(downloadDataSummary))

            val dateTime = "22 April 2024 10:05"
            val file     = s"""<strong class="govuk-tag">File not ready</strong>"""

            val tableRows = Seq(
              Seq(
                TableRow(
                  content = Text(dateTime)
                ),
                TableRow(
                  content = HtmlContent(file)
                )
              )
            )

            FileManagementTable.PendingFilesTable(tableParameter).value.pendingFileRows mustBe tableRows
          }
        }
      }
    }
  }
}