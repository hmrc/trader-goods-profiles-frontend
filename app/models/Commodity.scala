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

import play.api.libs.json.*
import play.api.libs.functional.syntax.*

import java.time.{Instant, LocalDate, ZoneId}

case class Commodity(
  commodityCode: String,
  descriptions: List[String],
  validityStartDate: Instant,
  validityEndDate: Option[Instant]
) {
  private val todayInstant: Instant = LocalDate.now(ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC")).toInstant
  
  def isValid: Boolean = validityEndDate.forall(validityEndDate => todayInstant.isAfter(validityStartDate) && todayInstant.isBefore(validityEndDate))
}

object Commodity {

  val reads: Reads[Commodity] = {

    def extractDescriptions(json: JsValue): List[String] = {
      val description        = (json \ "data" \ "attributes" \ "description").asOpt[String].toList
      val included           = (json \ "included").asOpt[JsArray].getOrElse(JsArray())
      val headingDescription = included.value.collect {
        case obj if (obj \ "type").asOpt[String].contains("heading") =>
          (obj \ "attributes" \ "description").asOpt[String]
      }.flatten

      val subheadingDescription = included.value.collect {
        case obj if (obj \ "type").asOpt[String].contains("commodity") =>
          (obj \ "attributes" \ "description").asOpt[String]
      }.flatten

      description ++ headingDescription ++ subheadingDescription
    }

    (
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").read[String] and
        __.read[JsValue].map(extractDescriptions) and
        (__ \ "data" \ "attributes" \ "validity_start_date").read[Instant] and
        (__ \ "data" \ "attributes" \ "validity_end_date").readNullable[Instant]
    )(Commodity.apply _)
  }

  val writes: OWrites[Commodity] = {

    def writeDescriptions(descriptions: List[String]): JsValue = {
      val description = descriptions.headOption
        .map { desc =>
          Json.obj("data" -> Json.obj("attributes" -> Json.obj("description" -> desc)))
        }
        .getOrElse(Json.obj())
      val included = {
        val heading = descriptions.lift(1).map { desc =>
          Json.obj(
            "type"       -> "heading",
            "attributes" -> Json.obj("description" -> desc)
          )
        }

        val subheading = descriptions.lift(2).map { desc =>
          Json.obj(
            "type"       -> "commodity",
            "attributes" -> Json.obj("description" -> desc)
          )
        }

        Json.arr(heading, subheading)
      }

      description ++ Json.obj("included" -> included)

    }

    (
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").write[String] and
        __.write[JsValue].contramap(writeDescriptions) and
        (__ \ "data" \ "attributes" \ "validity_start_date").write[Instant] and
        (__ \ "data" \ "attributes" \ "validity_end_date").writeOptionWithNull[Instant]
    )(o => Tuple.fromProductTyped(o))
  }

  implicit val format: OFormat[Commodity] = OFormat(reads, writes)
}
