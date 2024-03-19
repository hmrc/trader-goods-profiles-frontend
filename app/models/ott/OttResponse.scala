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

import play.api.libs.json._

case class OttResponse(
  data: JsObject,
  included: List[JsObject]
) {
  def getApplicableCategoryAssessments(): Option[List[CategoryAssessment]] = {
    val applicableIds = (data \ "relationships" \ "applicable_category_assessments" \ "data").asOpt[List[JsObject]] match {
      case Some(list) => list.map(obj => (obj \ "id").as[String])
      case None => List.empty[String]
    }

    val assessments = included.filter(obj => applicableIds.contains((obj \ "id").as[String]))

    Some(assessments.map { assessment =>
      val measureIds = (assessment \ "relationships" \ "measures" \ "data").asOpt[List[JsObject]] match {
        case Some(list) => list.map(obj => (obj \ "id").as[String])
        case None => List.empty[String]
      }
      val measures = included.filter(obj => measureIds.contains((obj \ "id").as[String]))
      val measuresModelled = measures.map { measure =>
        Measure((measure \ "id").as[String])
      }

      CategoryAssessment((assessment \ "id").as[String], Some(measuresModelled))
    })
  }
}

object OttResponse {
  implicit val ottResponseFormat = Json.format[OttResponse]
}