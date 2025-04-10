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

import models.GoodsRecord
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant

case class CreateRecordRequest(
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  category: Option[Int]
)

object CreateRecordRequest {

  def map(goodsRecord: GoodsRecord): CreateRecordRequest =
    CreateRecordRequest(
      goodsRecord.eori,
      goodsRecord.eori,
      goodsRecord.traderRef,
      goodsRecord.commodity.commodityCode,
      goodsRecord.goodsDescription,
      goodsRecord.countryOfOrigin,
      goodsRecord.commodity.validityStartDate,
      goodsRecord.commodity.validityEndDate,
      None
    )

  implicit val reads: Reads[CreateRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "traderRef").read[String] and
      (JsPath \ "comcode").read[String] and
      (JsPath \ goodsDescriptionKey).read[String] and
      (JsPath \ countryOfOriginKey).read[String] and
      (JsPath \ "comcodeEffectiveFromDate").read[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant] and
      (JsPath \ "category").readNullable[Int])(CreateRecordRequest.apply _)

  implicit lazy val writes: OWrites[CreateRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "traderRef").write[String] and
      (JsPath \ "comcode").write[String] and
      (JsPath \ goodsDescriptionKey).write[String] and
      (JsPath \ countryOfOriginKey).write[String] and
      (JsPath \ "comcodeEffectiveFromDate").write[Instant] and
      (JsPath \ "comcodeEffectiveToDate").writeNullable[Instant] and
      (JsPath \ "category").writeNullable[Int])(o => Tuple.fromProductTyped(o))
}
