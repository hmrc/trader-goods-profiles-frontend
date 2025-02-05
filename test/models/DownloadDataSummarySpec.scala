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
import models.DownloadDataStatus.FileInProgress
import play.api.libs.json.*

import java.time.Instant

class DownloadDataSummarySpec extends SpecBase {

  private val time: Instant = Instant.now()

  private val fileInfo: FileInfo = FileInfo("file.txt", 100, "7")

  private val downloadDataSummary: DownloadDataSummary = DownloadDataSummary(
    "summaryId",
    "eori",
    FileInProgress,
    time,
    time,
    Some(fileInfo)
  )

  val jsObj: JsObject = Json.obj(
    "summaryId" -> "summaryId",
    "eori"      -> "eori",
    "status"    -> "FileInProgress",
    "createdAt" -> time,
    "expiresAt" -> time,
    "fileInfo"  -> Json.obj(
      "fileName"      -> "file.txt",
      "fileSize"      -> 100,
      "retentionDays" -> "7"
    )
  )

  "DownloadDataSummary" - {
    "must deserialize from json" in {
      Json.fromJson[DownloadDataSummary](jsObj) mustBe JsSuccess(downloadDataSummary)
    }

    "must serialize to json" in {
      Json.toJson(downloadDataSummary) mustBe jsObj
    }
  }

}
