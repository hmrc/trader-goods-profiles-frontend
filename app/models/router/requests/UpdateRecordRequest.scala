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

import models.{CategoryRecord, SupplementaryRequest, UpdateGoodsRecord}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import scala.util.Try
import play.api.libs.json.{JsPath, OWrites, Reads}
import java.time.Instant
import scala.Function.unlift

case class UpdateRecordRequest(
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

object UpdateRecordRequest {

  def map(goodsRecord: UpdateGoodsRecord): UpdateRecordRequest =
    UpdateRecordRequest(
      goodsRecord.eori,
      goodsRecord.recordId,
      goodsRecord.eori,
      goodsRecord.countryOfOrigin,
      goodsRecord.goodsDescription,
      goodsRecord.traderReference,
      goodsRecord.commodityCode.map(_.commodityCode),
      goodsRecord.category,
      goodsRecord.commodityCodeStartDate,
      goodsRecord.commodityCodeEndDate
    )

  def mapFromCategoryAndComcode(categoryRecord: CategoryRecord): UpdateRecordRequest =
    UpdateRecordRequest(
      categoryRecord.eori,
      categoryRecord.recordId,
      categoryRecord.eori,
      category = Some(categoryRecord.category),
      comcode = categoryRecord.comcode,
      supplementaryUnit = convertToBigDecimal(categoryRecord.supplementaryUnit),
      measurementUnit = categoryRecord.measurementUnit
    )

  def mapFromSupplementary(supplementaryUnitRequest: SupplementaryRequest): UpdateRecordRequest =
    UpdateRecordRequest(
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

  implicit val reads: Reads[UpdateRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "recordId").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "countryOfOrigin").readNullable[String] and
      (JsPath \ "goodsDescription").readNullable[String] and
      (JsPath \ "traderRef").readNullable[String] and
      (JsPath \ "comcode").readNullable[String] and
      (JsPath \ "category").readNullable[Int] and
      (JsPath \ "comcodeEffectiveFromDate").readNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant] and
      (JsPath \ "supplementaryUnit").readNullable[BigDecimal] and
      (JsPath \ "measurementUnit").readNullable[String])(UpdateRecordRequest.apply _)

  implicit lazy val writes: OWrites[UpdateRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "countryOfOrigin").writeNullable[String] and
      (JsPath \ "goodsDescription").writeNullable[String] and
      (JsPath \ "traderRef").writeNullable[String] and
      (JsPath \ "comcode").writeNullable[String] and
      (JsPath \ "category").writeNullable[Int] and
      (JsPath \ "comcodeEffectiveFromDate").writeNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").writeNullable[Instant] and
      (JsPath \ "supplementaryUnit").writeNullable[BigDecimal] and
      (JsPath \ "measurementUnit").writeNullable[String])(unlift(UpdateRecordRequest.unapply))

  private def convertToBigDecimal(value: Option[String]): Option[BigDecimal] =
    value.flatMap(v => Try(BigDecimal(v)).toOption)
}
