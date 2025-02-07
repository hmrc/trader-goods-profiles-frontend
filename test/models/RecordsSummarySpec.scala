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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

class RecordsSummarySpec extends SpecBase {
  private val time: Instant          = Instant.parse("2025-02-05T15:05:07.950065061Z")
  private val convertedTime: Instant = Instant.parse("2025-02-05T15:05:07.950Z")

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val recordsSummaryUpdate: RecordsSummary.Update = RecordsSummary.Update(1, 2)
  private val recordsSummaryUpdateJson: JsObject          = Json.obj("recordsStored" -> 1, "totalRecords" -> 2)

  private val recordsSummary: RecordsSummary = RecordsSummary("eori", Some(recordsSummaryUpdate), convertedTime)
  private val recordsSummaryJson: JsObject   = Json.obj(
    "eori"          -> "eori",
    "currentUpdate" -> recordsSummaryUpdateJson,
    "lastUpdated"   -> Json.obj(
      "$date" -> Json.obj(
        "$numberLong" -> time.toEpochMilli.toString
      )
    )
  )

  "RecordsSummary" - {
    "must deserialize from json" in {
      Json.fromJson[RecordsSummary](recordsSummaryJson) mustBe JsSuccess(recordsSummary)
    }

    "must serialize to json" in {
      Json.toJson(recordsSummary) mustBe recordsSummaryJson
    }
  }
}
