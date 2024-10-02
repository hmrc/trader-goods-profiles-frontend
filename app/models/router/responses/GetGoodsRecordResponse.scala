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

import play.api.libs.json.{JsSuccess, JsValue, Json, Reads, Writes}
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey, niphlNumberKey, nirmsNumberKey, ukimsNumberKey}

import java.time.Instant

case class GetGoodsRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  adviceStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int],
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant] = None,
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String] = None,
  declarable: String,
  ukimsNumber: Option[String] = None,
  nirmsNumber: Option[String] = None,
  niphlNumber: Option[String] = None,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object GetGoodsRecordResponse {
  implicit val reads: Reads[GetGoodsRecordResponse] = (json: JsValue) =>
    JsSuccess(
      GetGoodsRecordResponse(
        (json \ "recordId").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "adviceStatus")
          .as[String],
        (json \ goodsDescriptionKey).as[String],
        (json \ countryOfOriginKey).as[String],
        (json \ "category").asOpt[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[BigDecimal],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ ukimsNumberKey).asOpt[String],
        (json \ nirmsNumberKey).asOpt[String],
        (json \ niphlNumberKey).asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val writes: Writes[GetGoodsRecordResponse] = (record: GetGoodsRecordResponse) =>
    Json.obj(
      "recordId"                 -> record.recordId,
      "eori"                     -> record.eori,
      "actorId"                  -> record.actorId,
      "traderRef"                -> record.traderRef,
      "comcode"                  -> record.comcode,
      "adviceStatus"             -> record.adviceStatus,
      goodsDescriptionKey        -> record.goodsDescription,
      countryOfOriginKey         -> record.countryOfOrigin,
      "category"                 -> record.category,
      "assessments"              -> record.assessments,
      "supplementaryUnit"        -> record.supplementaryUnit,
      "measurementUnit"          -> record.measurementUnit,
      "comcodeEffectiveFromDate" -> record.comcodeEffectiveFromDate,
      "comcodeEffectiveToDate"   -> record.comcodeEffectiveToDate,
      "version"                  -> record.version,
      "active"                   -> record.active,
      "toReview"                 -> record.toReview,
      "reviewReason"             -> record.reviewReason,
      "declarable"               -> record.declarable,
      ukimsNumberKey             -> record.ukimsNumber,
      nirmsNumberKey             -> record.nirmsNumber,
      niphlNumberKey             -> record.niphlNumber,
      "createdDateTime"          -> record.createdDateTime,
      "updatedDateTime"          -> record.updatedDateTime
    )
}
