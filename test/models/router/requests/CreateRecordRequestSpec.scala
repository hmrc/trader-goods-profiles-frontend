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
import play.api.libs.json.*

import java.time.Instant

class CreateRecordRequestSpec extends SpecBase {

  val time: Instant = Instant.now()

  val createRecordRequest: CreateRecordRequest =
    CreateRecordRequest(
      "eori",
      "actorId",
      "traderRef",
      "12012000",
      "goodsDesc",
      "GB",
      time,
      Some(time),
      Some(1)
    )

  val createRecordRequestJson: JsObject = Json.obj(
    "eori"                     -> "eori",
    "actorId"                  -> "actorId",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  "CreateRecordRequest" - {
    "must deserialize from json" in {
      Json.fromJson[CreateRecordRequest](createRecordRequestJson) mustBe JsSuccess(createRecordRequest)
    }

    "must serialize to json" in {
      Json.toJson(createRecordRequest) mustBe createRecordRequestJson
    }
  }

}
