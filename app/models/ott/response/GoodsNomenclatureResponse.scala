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
  descriptions: List[String]
)

object GoodsNomenclatureResponse {

  def extractDescriptions(json: JsValue): List[String] = {

    val includedDescriptions = (json \ "included")
      .asOpt[JsArray]
      .getOrElse(JsArray())
      .value
      .collect {
        case obj if (obj \ "type").asOpt[String].contains("goods_nomenclature") =>
          (obj \ "attributes" \ "description").asOpt[String]
      }
      .flatten
      .toList
    val mainDescription      = (json \ "data" \ "attributes" \ "description").asOpt[String].toList
    if (includedDescriptions.length >= 2) {
      includedDescriptions.takeRight(2) ++ mainDescription
    } else {
      includedDescriptions ++ mainDescription
    }
  }

  implicit lazy val reads: Reads[GoodsNomenclatureResponse] = (
    (__ \ "data" \ "id").read[String] and
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").read[String] and
      (__ \ "data" \ "attributes" \ "supplementary_measure_unit").readNullable[String] and
      (__ \ "data" \ "attributes" \ "validity_start_date").read[Instant] and
      (__ \ "data" \ "attributes" \ "validity_end_date").readNullable[Instant] and
      __.read[JsValue].map(extractDescriptions)
  )(GoodsNomenclatureResponse.apply _)
}
