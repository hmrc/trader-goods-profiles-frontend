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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, JsValue, Reads, Writes}

case class OttResponse(id: String, attributes: Attributes, relationships: Relationships)

case class Relationships(
   descendants: Option[JsValue],
   applicableCategoryAssessments: Option[JsValue],
   descendantCategoryAssessments: Option[JsValue],
   ancestors: Option[JsValue],
   measures: Option[JsValue]
)

case class Attributes(
  goodsNomenclatureItemId: String,
  parentSid: Option[String],
  description: String,
  numberIndents: Option[Int],
  productlineSuffix: Option[String],
  validityStartDate: JsValue, // You may want to define a specific type for dates
  validityEndDate: JsValue
)

object OttResponse {
  implicit val relationshipsWrites: Writes[Relationships] = (
        (JsPath \ "descendants").writeNullable[JsValue] and
        (JsPath \ "applicable_category_assessments").writeNullable[JsValue] and
        (JsPath \ "descendant_category_assessments").writeNullable[JsValue] and
        (JsPath \ "ancestors").writeNullable[JsValue] and
        (JsPath \ "measures").writeNullable[JsValue]
    )(unlift(Relationships.unapply))

  implicit val relationshipsReads: Reads[Relationships] = (
    (JsPath \ "descendants").readNullable[JsValue] and
      (JsPath \ "applicable_category_assessments").readNullable[JsValue] and
      (JsPath \ "descendant_category_assessments").readNullable[JsValue] and
      (JsPath \ "ancestors").readNullable[JsValue] and
      (JsPath \ "measures").readNullable[JsValue]
    )(Relationships.apply _)

  implicit val attributesWrites: Writes[Attributes] = (
    (JsPath \ "goods_nomenclature_item_id").write[String] and
      (JsPath \ "parent_sid").writeNullable[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "number_indents").writeNullable[Int] and
      (JsPath \ "productline_suffix").writeNullable[String] and
      (JsPath \ "validity_start_date").write[JsValue] and
      (JsPath \ "validity_end_date").write[JsValue]
    )(unlift(Attributes.unapply))

  implicit val attributesReads: Reads[Attributes] = (
    (JsPath \ "goods_nomenclature_item_id").read[String] and
      (JsPath \ "parent_sid").readNullable[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "number_indents").readNullable[Int] and
      (JsPath \ "productline_suffix").readNullable[String] and
      (JsPath \ "validity_start_date").read[JsValue] and
      (JsPath \ "validity_end_date").read[JsValue]
    )(Attributes.apply _)

  implicit val ottResponseWrites: Writes[OttResponse] = (
    (JsPath \ "data" \ "id").write[String] and
      (JsPath \ "data" \ "attributes").write[Attributes] and
      (JsPath \ "data" \ "relationships").write[Relationships]
    )(unlift(OttResponse.unapply))

  implicit val ottResponseReads: Reads[OttResponse] = (
    (JsPath \ "data" \ "id").read[String] and
      (JsPath \ "data" \ "attributes").read[Attributes] and
      (JsPath \ "data" \ "relationships").read[Relationships]
    )(OttResponse.apply _)
}
