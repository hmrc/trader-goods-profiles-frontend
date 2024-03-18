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

package models.ott

import play.api.libs.json.{JsValue, Json, OFormat, Reads, __}
import play.api.libs.functional.syntax._

case class CategoryAssessment(
 id: String,
 category: Option[JsValue],
 theme: Option[JsValue],
 geographical_area: Option[JsValue],
 excluded_geographical_areas: Option[JsValue],
 exemptions: Option[JsValue],
 measures: Option[List[IdTypePair]],
 included: Option[List[JsValue]]
) {
  def getRelatedMeasures(): Option[List[Measure]] = {
    (measures, included) match {
      case (Some(measureList), Some(includedList)) =>
        Some(measureList.flatMap { jsValue =>
          includedList.find(_.as[IdTypePair].id == jsValue.id).map { jsValue =>
            val measure = jsValue.as[Measure]
            val measure_with_included = measure.copy(included = Some(includedList))
            measure_with_included
          }
        })
      case _ => None
    }
  }
}

object CategoryAssessment {
  implicit val categoryAssessmentReads: Reads[CategoryAssessment] = (
    (__ \ "id").read[String] and
      (__ \ "attributes" \ "category").readNullable[JsValue] and
      (__ \ "attributes" \ "theme").readNullable[JsValue] and
      (__ \ "relationships" \ "geographical_area").readNullable[JsValue] and
      (__ \ "relationships" \ "excluded_geographical_areas").readNullable[JsValue] and
      (__ \ "relationships" \ "exemptions").readNullable[JsValue] and
      (__ \ "relationships" \ "measures" \ "data").readNullable[List[IdTypePair]] and
      (__ \ "included").readNullable[List[JsValue]]
    )(CategoryAssessment.apply _)
}