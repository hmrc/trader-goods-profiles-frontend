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

import models.{CategoryRecord, CategoryRecord2, Scenario2, UpdateGoodsRecord}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}

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
  supplementaryUnit: Option[Double] = None,
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
      goodsRecord.category
    )

  def mapFromCategoryAndComcode(categoryRecord: CategoryRecord): UpdateRecordRequest =
    UpdateRecordRequest(
      categoryRecord.eori,
      categoryRecord.recordId,
      categoryRecord.eori,
      category = Some(categoryRecord.category),
      comcode = categoryRecord.comcode,
      supplementaryUnit = convertToDouble(categoryRecord.supplementaryUnit),
      measurementUnit = categoryRecord.measurementUnit
    )

  def mapFromCategoryAndComcode2(categoryRecord: CategoryRecord2): UpdateRecordRequest =
    UpdateRecordRequest(
      categoryRecord.eori,
      categoryRecord.recordId,
      categoryRecord.eori,
      category = Some(Scenario2.getResultAsInt(categoryRecord.category)),
      comcode = Some(categoryRecord.comcode),
      //supplementaryUnit = convertToDouble(categoryRecord.supplementaryUnit),
      //measurementUnit = categoryRecord.measurementUnit
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
      (JsPath \ "supplementaryUnit").readNullable[Double] and
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
      (JsPath \ "supplementaryUnit").writeNullable[Double] and
      (JsPath \ "measurementUnit").writeNullable[String])(unlift(UpdateRecordRequest.unapply))

  private def convertToDouble(value: Option[String]): Option[Double] =
    value.map(_.toDouble)
}
