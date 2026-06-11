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
import models.{Commodity, SupplementaryRequest, UpdateGoodsRecord}
import play.api.libs.json.*

import java.time.Instant

class PatchRecordRequestSpec extends SpecBase {

  val time: Instant = Instant.now()

  val patchRecordRequest: PatchRecordRequest =
    PatchRecordRequest(
      "eori",
      "recordId",
      "actorId",
      Some("GB"),
      Some("goodsDesc"),
      Some("traderRef"),
      Some("12012000"),
      Some(1),
      Some(time),
      Some(time),
      Some(1.1),
      Some("kg")
    )

  val patchRecordRequestJson: JsObject = Json.obj(
    "eori"                     -> "eori",
    "actorId"                  -> "actorId",
    "recordId"                 -> "recordId",
    "traderRef"                -> "traderRef",
    "comcode"                  -> "12012000",
    "goodsDescription"         -> "goodsDesc",
    "countryOfOrigin"          -> "GB",
    "category"                 -> 1,
    "supplementaryUnit"        -> 1.1,
    "measurementUnit"          -> "kg",
    "comcodeEffectiveFromDate" -> time.toString,
    "comcodeEffectiveToDate"   -> time.toString
  )

  "PatchRecordRequest" - {
    "must deserialize from json" in {
      Json.fromJson[PatchRecordRequest](patchRecordRequestJson) mustBe JsSuccess(patchRecordRequest)
    }

    "must serialize to json" in {
      Json.toJson(patchRecordRequest) mustBe patchRecordRequestJson
    }

    "map" - {
      "must map from UpdateGoodsRecord" in {
        val commodity    = Commodity("12012000", List.empty, time, None)
        val updateRecord = UpdateGoodsRecord(
          eori = "eori",
          recordId = "recordId",
          countryOfOrigin = Some("GB"),
          goodsDescription = Some("goodsDesc"),
          productReference = Some("traderRef"),
          commodityCode = Some(commodity),
          category = Some(1),
          commodityCodeStartDate = Some(time),
          commodityCodeEndDate = Some(time)
        )
        val result       = PatchRecordRequest.map(updateRecord)
        result.eori mustBe "eori"
        result.recordId mustBe "recordId"
        result.countryOfOrigin mustBe Some("GB")
        result.comcode mustBe Some("12012000")
      }
    }

    "mapFromSupplementary" - {
      "must map with a valid supplementary unit" in {
        val supplementary =
          SupplementaryRequest("eori", "recordId", supplementaryUnit = Some("1.5"), measurementUnit = Some("kg"))
        val result        = PatchRecordRequest.mapFromSupplementary(supplementary)
        result.supplementaryUnit mustBe Some(BigDecimal("1.5"))
        result.measurementUnit mustBe Some("kg")
      }

      "must map with no supplementary unit" in {
        val supplementary = SupplementaryRequest("eori", "recordId", supplementaryUnit = None)
        val result        = PatchRecordRequest.mapFromSupplementary(supplementary)
        result.supplementaryUnit mustBe Some(0)
      }

      "must map with a non-numeric supplementary unit" in {
        val supplementary = SupplementaryRequest("eori", "recordId", supplementaryUnit = Some("invalid"))
        val result        = PatchRecordRequest.mapFromSupplementary(supplementary)
        result.supplementaryUnit mustBe None
      }
    }
  }

}
