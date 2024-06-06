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

package models.router

import play.api.libs.json._

import java.time.Instant

case class CreateOrUpdateRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  accreditationStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object CreateOrUpdateRecordResponse {

  implicit val reads: Reads[CreateOrUpdateRecordResponse] = (json: JsValue) =>
    JsSuccess(
      CreateOrUpdateRecordResponse(
        (json \ "recordId").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "accreditationStatus").as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[Int],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").asOpt[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val writes: Writes[CreateOrUpdateRecordResponse] = (response: CreateOrUpdateRecordResponse) =>
    Json.obj(
      "recordId"                 -> response.recordId,
      "eori"                     -> response.eori,
      "actorId"                  -> response.actorId,
      "traderRef"                -> response.traderRef,
      "comcode"                  -> response.comcode,
      "accreditationStatus"      -> response.accreditationStatus,
      "goodsDescription"         -> response.goodsDescription,
      "countryOfOrigin"          -> response.countryOfOrigin,
      "category"                 -> response.category,
      "assessments"              -> response.assessments,
      "supplementaryUnit"        -> response.supplementaryUnit,
      "measurementUnit"          -> response.measurementUnit,
      "comcodeEffectiveFromDate" -> response.comcodeEffectiveFromDate,
      "comcodeEffectiveToDate"   -> response.comcodeEffectiveToDate,
      "version"                  -> response.version,
      "active"                   -> response.active,
      "toReview"                 -> response.toReview,
      "reviewReason"             -> response.reviewReason,
      "declarable"               -> response.declarable,
      "ukimsNumber"              -> response.ukimsNumber,
      "nirmsNumber"              -> response.nirmsNumber,
      "niphlNumber"              -> response.niphlNumber,
      "createdDateTime"          -> response.createdDateTime,
      "updatedDateTime"          -> response.updatedDateTime
    )
}
