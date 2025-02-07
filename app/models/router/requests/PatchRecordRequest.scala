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

import models.{CategoryRecord, Scenario, SupplementaryRequest, UpdateGoodsRecord}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant
import scala.util.Try

case class PatchRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  countryOfOrigin: Option[String] = None,
  goodsDescription: Option[String] = None,
  traderRef: Option[String] = None,
  comcode: Option[String] = None,
  category: Option[Int] = None,
  comcodeEffectiveFromDate: Option[Instant] = None,
  comcodeEffectiveToDate: Option[Instant] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String] = None
)

object PatchRecordRequest {

  def map(goodsRecord: UpdateGoodsRecord): PatchRecordRequest =
    PatchRecordRequest(
      goodsRecord.eori,
      goodsRecord.recordId,
      goodsRecord.eori,
      goodsRecord.countryOfOrigin,
      goodsRecord.goodsDescription,
      goodsRecord.productReference,
      goodsRecord.commodityCode.map(_.commodityCode),
      goodsRecord.category,
      goodsRecord.commodityCodeStartDate,
      goodsRecord.commodityCodeEndDate
    )

  def mapFromCategoryAndComcode(categoryRecord: CategoryRecord): PatchRecordRequest =
    PatchRecordRequest(
      categoryRecord.eori,
      categoryRecord.recordId,
      categoryRecord.eori,
      category = Some(Scenario.getResultAsInt(categoryRecord.category)),
      comcode = Some(categoryRecord.finalComCode),
      supplementaryUnit = convertToBigDecimal(categoryRecord.supplementaryUnit),
      measurementUnit = categoryRecord.measurementUnit
    )

  def mapFromSupplementary(supplementaryUnitRequest: SupplementaryRequest): PatchRecordRequest =
    PatchRecordRequest(
      supplementaryUnitRequest.eori,
      supplementaryUnitRequest.recordId,
      supplementaryUnitRequest.eori,
      supplementaryUnit = supplementaryUnitRequest.supplementaryUnit match {
        case s if s.nonEmpty => convertToBigDecimal(supplementaryUnitRequest.supplementaryUnit)
        //API don't support removing supplementaryUnit, so setting it to zero here
        case _               => Some(0)
      },
      measurementUnit = supplementaryUnitRequest.measurementUnit
    )

  implicit val reads: Reads[PatchRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "recordId").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ countryOfOriginKey).readNullable[String] and
      (JsPath \ goodsDescriptionKey).readNullable[String] and
      (JsPath \ "traderRef").readNullable[String] and
      (JsPath \ "comcode").readNullable[String] and
      (JsPath \ "category").readNullable[Int] and
      (JsPath \ "comcodeEffectiveFromDate").readNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant] and
      (JsPath \ "supplementaryUnit").readNullable[BigDecimal] and
      (JsPath \ "measurementUnit").readNullable[String])(PatchRecordRequest.apply _)

  implicit lazy val writes: OWrites[PatchRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ countryOfOriginKey).writeNullable[String] and
      (JsPath \ goodsDescriptionKey).writeNullable[String] and
      (JsPath \ "traderRef").writeNullable[String] and
      (JsPath \ "comcode").writeNullable[String] and
      (JsPath \ "category").writeNullable[Int] and
      (JsPath \ "comcodeEffectiveFromDate").writeNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").writeNullable[Instant] and
      (JsPath \ "supplementaryUnit").writeNullable[BigDecimal] and
      (JsPath \ "measurementUnit").writeNullable[String])(o => Tuple.fromProductTyped(o))

  private def convertToBigDecimal(value: Option[String]): Option[BigDecimal] =
    value.flatMap(v => Try(BigDecimal(v)).toOption)
}
