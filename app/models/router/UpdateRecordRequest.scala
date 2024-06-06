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

import models.GoodsRecord
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}

import java.time.Instant
import scala.Function.unlift

case class UpdateRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  traderRef: Option[String],
  comcode: Option[String],
  goodsDescription: Option[String],
  countryOfOrigin: Option[String],
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Option[Instant],
  comcodeEffectiveToDate: Option[Instant]
)

object UpdateRecordRequest {

  def map(goodsRecord: GoodsRecord): UpdateRecordRequest =
    UpdateRecordRequest(
      goodsRecord.eori,
      goodsRecord.recordId,
      goodsRecord.eori,
      Some(goodsRecord.traderRef),
      Some(goodsRecord.comcode),
      Some(goodsRecord.goodsDescription),
      Some(goodsRecord.countryOfOrigin),
      goodsRecord.category,
      goodsRecord.assessments,
      goodsRecord.supplementaryUnit,
      goodsRecord.measurementUnit,
      Some(goodsRecord.comcodeEffectiveFromDate),
      goodsRecord.comcodeEffectiveToDate
    )

  implicit val reads: Reads[UpdateRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "recordId").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "traderRef").readNullable[String] and
      (JsPath \ "comcode").readNullable[String] and
      (JsPath \ "goodsDescription").readNullable[String] and
      (JsPath \ "countryOfOrigin").readNullable[String] and
      (JsPath \ "category").readNullable[Int] and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[Int] and
      (JsPath \ "measurementUnit").readNullable[String] and
      (JsPath \ "comcodeEffectiveFromDate").readNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant])(UpdateRecordRequest.apply _)

  implicit lazy val writes: OWrites[UpdateRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "traderRef").writeNullable[String] and
      (JsPath \ "comcode").writeNullable[String] and
      (JsPath \ "goodsDescription").writeNullable[String] and
      (JsPath \ "countryOfOrigin").writeNullable[String] and
      (JsPath \ "category").writeNullable[Int] and
      (JsPath \ "assessments").writeNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").writeNullable[Int] and
      (JsPath \ "measurementUnit").writeNullable[String] and
      (JsPath \ "comcodeEffectiveFromDate").writeNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").writeNullable[Instant])(unlift(UpdateRecordRequest.unapply))
}
