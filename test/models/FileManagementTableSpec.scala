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
import helpers.FileManagementTableComponentHelper
import models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import models.filemanagement._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow

import java.time.Instant

class FileManagementTableSpec extends SpecBase with Generators {

  "FileManagementTable" - {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    implicit val fileManagementTableComponentHelper: FileManagementTableComponentHelper =
      application.injector.instanceOf[FileManagementTableComponentHelper]

    implicit val message: Messages = messages(application)

    val tableRows = arbitrarySeqTableRows.sample.value

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
            AvailableFilesTable(None) mustBe None
          }

          "when populated with both DownloadDataSummary and DownloadData" in {

            val fileCreated = Instant.parse("2024-04-22T10:05:00Z")
            val fileExpiry  = Instant.parse("2024-05-22T10:05:00Z")

            val fileName            = "url.csv"
            val fileInfo            = FileInfo(fileName, 1, "30")
            val downloadDataSummary =
              DownloadDataSummary("id", "testEori", FileReadyUnseen, fileCreated, fileExpiry, Some(fileInfo))
            val downloadData        = DownloadData(fileName, fileName, 1, Seq.empty)

            val tableParameter = Some(Seq((downloadDataSummary, downloadData)))

            val dateTime   = "22 April 2024 10:05am"
            val expiryDate = "22 May 2024 10:05am"
            val file       = fileManagementTableComponentHelper.createLink(
              "Download file",
              "requested on 22 April 2024 10:05am",
              fileName
            )

            val tableRows = Seq(
              Seq(
                TableRow(
                  content = Text(dateTime)
                ),
                TableRow(
                  content = Text(expiryDate)
                ),
                TableRow(
                  content = file
                )
              )
            )

            AvailableFilesTable(tableParameter).value.availableFileRows mustBe tableRows
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
            PendingFilesTable(None) mustBe None
          }

          "when populated with DownloadDataSummary" in {

            val instant = Instant.parse("2024-04-22T10:05:00Z")

            val downloadDataSummary =
              DownloadDataSummary("id", "testEori", FileInProgress, instant, Instant.now(), None)

            val tableParameter = Some(Seq(downloadDataSummary))

            val dateTime = "22 April 2024 10:05am"
            val file     = fileManagementTableComponentHelper.createTag("File not ready")

            val tableRows = Seq(
              Seq(
                TableRow(
                  content = Text(dateTime)
                ),
                TableRow(
                  content = file
                )
              )
            )

            PendingFilesTable(tableParameter).value.pendingFileRows mustBe tableRows
          }
        }
      }
    }
  }
}
