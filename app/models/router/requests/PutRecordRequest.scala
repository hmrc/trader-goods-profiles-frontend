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

import models.router.responses.{Assessment, GetGoodsRecordResponse}
import models.{CategoryRecord, Scenario, SupplementaryRequest}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import scala.util.Try

case class PutRecordRequest(
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant]
)

object PutRecordRequest {
  implicit val format: OFormat[PutRecordRequest] = Json.format[PutRecordRequest]

  def mapFromCategoryAndComcode(categoryRecord: CategoryRecord, oldRecord: GetGoodsRecordResponse): PutRecordRequest =
    PutRecordRequest(
      categoryRecord.eori,
      oldRecord.traderRef,
      categoryRecord.finalComCode,
      oldRecord.goodsDescription,
      oldRecord.countryOfOrigin,
      Some(Scenario.getResultAsInt(categoryRecord.category)),
      oldRecord.assessments,
      convertToBigDecimal(categoryRecord.supplementaryUnit),
      categoryRecord.measurementUnit,
      oldRecord.comcodeEffectiveFromDate,
      oldRecord.comcodeEffectiveToDate
    )

  def mapFromSupplementary(
    supplementaryUnitRequest: SupplementaryRequest,
    oldRecord: GetGoodsRecordResponse
  ): PutRecordRequest =
    PutRecordRequest(
      actorId = supplementaryUnitRequest.eori,
      oldRecord.traderRef,
      oldRecord.comcode,
      oldRecord.goodsDescription,
      oldRecord.countryOfOrigin,
      oldRecord.category,
      oldRecord.assessments,
      supplementaryUnit = supplementaryUnitRequest.supplementaryUnit match {
        case s if s.nonEmpty => convertToBigDecimal(supplementaryUnitRequest.supplementaryUnit)
        //API don't support removing supplementaryUnit, so setting it to zero here
        case _               => Some(0)
      },
      measurementUnit = supplementaryUnitRequest.measurementUnit,
      oldRecord.comcodeEffectiveFromDate,
      oldRecord.comcodeEffectiveToDate
    )

  private def convertToBigDecimal(value: Option[String]): Option[BigDecimal] =
    value.flatMap(v => Try(BigDecimal(v)).toOption)
}
