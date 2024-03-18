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


case class Measure(
  id: String,
  goodsNomenclatureItemId: Option[JsValue],
  goodsNomenclatureSid: Option[JsValue],
  effectiveStartDate: Option[JsValue],
  effectiveEndDate: Option[JsValue],
  measureType: Option[JsValue],
  footnotes: Option[JsValue],
  included: Option[List[JsValue]]
)

object Measure {
  implicit val measureReads: Reads[Measure] = (
    (__ \ "id").read[String] and
      (__ \ "attributes" \ "goods_nomenclature_item_id").readNullable[JsValue] and
      (__ \ "attributes" \ "goods_nomenclature_sid").readNullable[JsValue] and
      (__ \ "attributes" \ "effective_start_date").readNullable[JsValue] and
      (__ \ "attributes" \ "effective_end_date").readNullable[JsValue] and
      (__ \ "relationships" \ "measure_type").readNullable[JsValue] and
      (__ \ "relationships" \ "footnotes").readNullable[JsValue] and
      (__ \ "included").readNullable[List[JsValue]]
    )(Measure.apply _)
}