/*
 * Copyright 2025 HM Revenue & Customs
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

import models.{DownloadDataStatus, DownloadDataSummary}
import play.api.i18n.Messages
import play.twirl.api.Html

case class DownloadLinkText(
  downloadDataSummaries: Seq[DownloadDataSummary],
  doesGoodsRecordExist: Boolean,
  verifiedEmail: Boolean
)(implicit messages: Messages) {

  val messageKey: String = if (!verifiedEmail) {
    "homepage.downloadLinkText.unverifiedEmail"
  } else if (downloadDataSummaries.isEmpty) {
    if (doesGoodsRecordExist) {
      "homepage.downloadLinkText.noFilesRequested"
    } else {
      "homepage.downloadLinkText.noGoodsRecords"
    }
  } else {
    val hasReady = downloadDataSummaries.exists { summary =>
      summary.status == DownloadDataStatus.FileReadySeen ||
      summary.status == DownloadDataStatus.FileReadyUnseen
    }
    if (hasReady) {
      if (doesGoodsRecordExist) {
        "homepage.downloadLinkText.filesRequested"
      } else {
        "homepage.downloadLinkText.downloadRecords"
      }
    } else {
      "homepage.downloadLinkText.filesFailed"
    }
  }

  val nonLinks: Seq[String]   =
    Seq("homepage.downloadLinkText.noGoodsRecords", "homepage.downloadLinkText.unverifiedEmail")
  private val isLink: Boolean = !nonLinks.contains(messageKey)

  val downloadLinkContent: Html = if (isLink) {
    Html(
      s"""<p class="govuk-body"><a href="${controllers.download.routes.FileManagementController
          .onPageLoad()}" class="govuk-link">${messages(messageKey)}</a></p>"""
    )
  } else {
    Html(s"""<p class="govuk-body">${messages(messageKey)}</p>""")
  }
}
