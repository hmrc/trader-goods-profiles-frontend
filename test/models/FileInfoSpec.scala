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
import play.api.libs.json.*

class FileInfoSpec extends SpecBase {

  val json: JsObject = Json.obj("fileName" -> "file.txt", "fileSize" -> 100, "retentionDays" -> "7")

  "FileInfo" - {
    "must deserialize from json" in {
      Json.fromJson[FileInfo](json) mustBe JsSuccess(FileInfo("file.txt", 100, "7"))
    }

    "must serialize to json" in {
      Json.toJson(FileInfo("file.txt", 100, "7")) mustBe json
    }
  }

}
