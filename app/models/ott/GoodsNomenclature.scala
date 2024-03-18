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
import play.api.libs.functional.syntax._

case class IdTypePair(id: String, `type`: String)
object IdTypePair {
  implicit val idTypePairFormat: OFormat[IdTypePair] = Json.format[IdTypePair]
}

// Add fields and methods to this on an as-needed basis to maintain simplicity.
case class GoodsNomenclature(
  id: String,
  goods_nomenclature_item_id: String,
  description: String,
  applicable_category_assessments: Option[List[IdTypePair]],
  included: Option[List[JsValue]]
) {
  def getApplicableCategoryAssessments(): Option[List[CategoryAssessment]] = {
    (applicable_category_assessments, included) match {
      case (Some(assessments), Some(includedList)) =>
        Some(assessments.flatMap { jsValue =>
          includedList.find(_.as[IdTypePair].id == jsValue.id).map { jsValue =>
            val assessment = jsValue.as[CategoryAssessment]
            val assessment_with_included = assessment.copy(included = Some(includedList))
            assessment_with_included
          }
        })
      case _ => None
    }
  }
}

object GoodsNomenclature {
  implicit val goodsNomenclatureReads: Reads[GoodsNomenclature] = (
    (__ \ "data" \ "id").read[String] and
      (__ \ "data" \ "attributes" \ "goods_nomenclature_item_id").read[String] and
      (__ \ "data" \ "attributes" \ "description").read[String] and
      (__ \ "data" \ "relationships" \ "applicable_category_assessments" \ "data").readNullable[List[IdTypePair]] and
      (__ \ "included").readNullable[List[JsValue]]
    )(GoodsNomenclature.apply _)
}
