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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class DownloadDataSpec extends AnyFreeSpec with Matchers {

  "DownloadData JSON serialization" - {

    val metadataSeq = Seq(
      Metadata("source", "internal"),
      Metadata("type", "csv")
    )

    val downloadData = DownloadData(
      downloadURL = "https://example.com/download/file.csv",
      filename = "file.csv",
      fileSize = 1024,
      metadata = metadataSeq
    )

    val json = Json.obj(
      "downloadURL" -> "https://example.com/download/file.csv",
      "filename"    -> "file.csv",
      "fileSize"    -> 1024,
      "metadata"    -> Json.arr(
        Json.obj("metadata" -> "source", "value" -> "internal"),
        Json.obj("metadata" -> "type", "value"   -> "csv")
      )
    )

    "must serialize to JSON" in {
      Json.toJson(downloadData) mustBe json
    }

    "must deserialize from JSON" in {
      Json.fromJson[DownloadData](json) mustBe JsSuccess(downloadData)
    }
  }
}
