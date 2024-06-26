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

package models

import play.api.libs.json.OFormat
import play.api.libs.json._

import java.time.Instant

case class Commodity(
  commodityCode: String,
  description: String,
  validityStartDate: Instant,
  validityEndDate: Option[Instant]
)

object Commodity {

  val reads: Reads[Commodity] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").read[String] and
        (__ \ "data" \ "attributes" \ "description").read[String] and
        (__ \ "data" \ "attributes" \ "validity_start_date").read[Instant] and
        (__ \ "data" \ "attributes" \ "validity_end_date").readNullable[Instant]
    )(Commodity.apply _)
  }

  val writes: OWrites[Commodity] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").write[String] and
        (__ \ "data" \ "attributes" \ "description").write[String] and
        (__ \ "data" \ "attributes" \ "validity_start_date").write[Instant] and
        (__ \ "data" \ "attributes" \ "validity_end_date").writeOptionWithNull[Instant]
    )(unlift(Commodity.unapply))
  }

  implicit val format: OFormat[Commodity] = OFormat(reads, writes)
}
