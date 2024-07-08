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

package models.ott.response

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.Instant

final case class GoodsNomenclatureResponse(
  id: String,
  commodityCode: String,
  measurementUnit: Option[String],
  validityStartDate: Instant,
  validityEndDate: Option[Instant],
  description: String
) {
  private def truncateCommodityCode(commodityCode: String): String = {
    val lastFourDigits = commodityCode.takeRight(4)
    val lastTwoCodes = lastFourDigits.grouped(2).toSeq
    lastTwoCodes match {
      case Seq("00", "00") => commodityCode.dropRight(4)
      case Seq(_, "00") => commodityCode.dropRight(2)
      case _ => commodityCode
    }
  }

  def apply(
   id: String,
   commodityCode: String,
   measurementUnit: Option[String],
   validityStartDate: Instant,
   validityEndDate: Option[Instant],
   description: String
  ): GoodsNomenclatureResponse = {
    GoodsNomenclatureResponse(
      id,
      truncateCommodityCode(commodityCode),
      measurementUnit,
      validityStartDate,
      validityEndDate,
      description
    )
  }
}

object GoodsNomenclatureResponse {

  implicit lazy val reads: Reads[GoodsNomenclatureResponse] = (
    (__ \ "id").read[String] and
      (__ \ "attributes" \ "goods_nomenclature_item_id").read[String] and
      (__ \ "attributes" \ "supplementary_measure_unit").readNullable[String] and
      (__ \ "attributes" \ "validity_start_date").read[Instant] and
      (__ \ "attributes" \ "validity_end_date").readNullable[Instant] and
      (__ \ "attributes" \ "description").read[String]
  )(GoodsNomenclatureResponse.apply _)
}
