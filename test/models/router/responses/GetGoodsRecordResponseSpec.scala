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

package models.router.responses

import base.SpecBase
import models.{AdviceStatus, DeclarableStatus, ReviewReason}
import play.api.libs.json.*

import java.time.Instant

class GetGoodsRecordResponseSpec extends SpecBase {

  private val condition: Condition   =
    Condition(Some("type"), Some("conditionId"), Some("conditionDescription"), Some("conditionTraderText"))
  private val assessment: Assessment = Assessment(Some("1234"), Some(100), Some(condition))

  private val conditionJson: JsObject  =
    Json.obj(
      "type"                 -> "type",
      "conditionId"          -> "conditionId",
      "conditionDescription" -> "conditionDescription",
      "conditionTraderText"  -> "conditionTraderText"
    )
  private val assessmentJson: JsObject =
    Json.obj("assessmentId" -> "1234", "primaryCategory" -> 100, "condition" -> conditionJson)

  private val getGoodsRecordResponse   = GetGoodsRecordResponse(
    recordId = "recordId",
    eori = "eori",
    actorId = "actorId",
    traderRef = "traderRef",
    comcode = "comcode",
    adviceStatus = AdviceStatus.NotRequested,
    goodsDescription = "goodsDescription",
    countryOfOrigin = "countryOfOrigin",
    category = Some(1),
    assessments = Some(Seq(assessment)),
    supplementaryUnit = Some(BigDecimal(1.23)),
    measurementUnit = Some("kg"),
    comcodeEffectiveFromDate = Instant.parse("2023-01-01T00:00:00Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2023-12-31T23:59:59Z")),
    version = 1,
    active = true,
    toReview = true,
    reviewReason = Some(ReviewReason.Commodity),
    declarable = DeclarableStatus.ImmiReady,
    ukimsNumber = Some("ukimsNumber"),
    nirmsNumber = Some("nirmsNumber"),
    niphlNumber = Some("niphlNumber"),
    createdDateTime = Instant.parse("2023-01-01T00:00:00Z"),
    updatedDateTime = Instant.parse("2023-01-01T00:00:00Z")
  )

  private val getGoodsRecordResponseJson = Json.obj(
    "recordId"                 -> "recordId",
    "eori"                     -> "eori",
    "actorId"                  -> "actorId",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "comcode",
    "adviceStatus"             -> "Not Requested",
    "goodsDescription"         -> "goodsDescription",
    "countryOfOrigin"          -> "countryOfOrigin",
    "category"                 -> 1,
    "assessments"              -> Json.arr(assessmentJson),
    "reviewReason"             -> "commodity",
    "supplementaryUnit"        -> 1.23,
    "measurementUnit"          -> "kg",
    "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z",
    "comcodeEffectiveToDate"   -> "2023-12-31T23:59:59Z",
    "version"                  -> 1,
    "active"                   -> true,
    "toReview"                 -> true,
    "declarable"               -> "IMMI Ready",
    "ukimsNumber"              -> "ukimsNumber",
    "nirmsNumber"              -> "nirmsNumber",
    "niphlNumber"              -> "niphlNumber",
    "createdDateTime"          -> "2023-01-01T00:00:00Z",
    "updatedDateTime"          -> "2023-01-01T00:00:00Z"
  )

  "GetGoodsRecordResponse" - {
    "must deserialize from json" in {
      Json.fromJson[GetGoodsRecordResponse](getGoodsRecordResponseJson) mustBe JsSuccess(getGoodsRecordResponse)
    }

    "must serialize to json" in {
      Json.toJson(getGoodsRecordResponse) mustBe getGoodsRecordResponseJson
    }
  }

}
