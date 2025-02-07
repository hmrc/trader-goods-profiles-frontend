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

package models.router.requests

import base.SpecBase
import models.router.responses.{Assessment, Condition}
import play.api.libs.json.*

import java.time.Instant

class PutRecordRequestSpec extends SpecBase {

  val time: Instant = Instant.now()

  private val condition: Condition   =
    Condition(Some("type"), Some("conditionId"), Some("conditionDescription"), Some("conditionTraderText"))
  private val assessment: Assessment =
    Assessment(Some("1234"), Some(100), Some(condition))

  val putRecordRequest: PutRecordRequest =
    PutRecordRequest(
      "actorId",
      "traderRef",
      "12012000",
      "goodsDesc",
      "GB",
      Some(1),
      Some(Seq(assessment)),
      Some(1.1),
      Some("kg"),
      time,
      Some(time)
    )

  val putRecordRequestJson: JsObject = Json.obj(
    "actorId"                  -> "actorId",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "assessments"              -> Json.arr(
      Json.obj(
        "assessmentId"    -> "1234",
        "primaryCategory" -> 100,
        "condition"       -> Json.obj(
          "type"                 -> "type",
          "conditionId"          -> "conditionId",
          "conditionDescription" -> "conditionDescription",
          "conditionTraderText"  -> "conditionTraderText"
        )
      )
    ),
    "supplementaryUnit"        -> 1.1,
    "measurementUnit"          -> "kg",
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  "PutRecordRequest" - {
    "must deserialize from json" in {
      Json.fromJson[PutRecordRequest](putRecordRequestJson) mustBe JsSuccess(putRecordRequest)
    }

    "must serialize to json" in {
      Json.toJson(putRecordRequest) mustBe putRecordRequestJson
    }
  }

}
