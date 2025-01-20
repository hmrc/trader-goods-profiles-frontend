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
import models.DownloadDataStatus.FileReadySeen
import models.download._
import play.api.i18n.Messages

import java.time.Instant

class DownloadLinkTextSpec extends SpecBase with Generators {

  "DownloadLinkText" - {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    implicit val message: Messages = messages(application)

    "messageKey must return correct key" - {
      "when email is verified" - {
        "and goods record exists" - {
          "and downloadDataSummaries is not empty" in {
            val downloadDataSummary = DownloadDataSummary(
              "id",
              "eori",
              FileReadySeen,
              Instant.now(),
              Instant.now(),
              None
            )

            val downloadLinkText =
              DownloadLinkText(Seq(downloadDataSummary), doesGoodsRecordExist = true, verifiedEmail = true)
            downloadLinkText.messageKey mustBe "homepage.downloadLinkText.filesRequested"
          }

          "and downloadDataSummaries is empty" in {
            val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = true, verifiedEmail = true)
            downloadLinkText.messageKey mustBe "homepage.downloadLinkText.noFilesRequested"
          }
        }

        "and goods record does not exist" in {
          val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = false, verifiedEmail = true)
          downloadLinkText.messageKey mustBe "homepage.noRecords"
        }
      }

      "when email is not verified" in {
        val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = false, verifiedEmail = false)
        downloadLinkText.messageKey mustBe "homepage.downloadLinkText.unverifiedEmail"
      }
    }

    "nonLinks" - {
      "should contain correct messageKeys" in {
        val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = false, verifiedEmail = false)
        downloadLinkText.nonLinks mustBe Seq("homepage.noRecords", "homepage.downloadLinkText.unverifiedEmail")
      }
    }

    "downloadLinkContent" - {
      "should return correct html" - {
        "when isLink is true" in {
          val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = true, verifiedEmail = true)
          downloadLinkText.downloadLinkContent.toString mustBe
            s"""<p class="govuk-body"><a href="${controllers.download.routes.FileManagementController
              .onPageLoad()}" class="govuk-link">${message(downloadLinkText.messageKey)}</a></p>"""
        }

        "when isLink is false" in {
          val downloadLinkText = DownloadLinkText(Seq.empty, doesGoodsRecordExist = false, verifiedEmail = false)
          downloadLinkText.downloadLinkContent.toString mustBe s"""<p class="govuk-body">${message(
            downloadLinkText.messageKey
          )}</p>"""
        }
      }
    }
  }
}
