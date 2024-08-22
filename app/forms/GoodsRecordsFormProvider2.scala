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

package forms

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}

class GoodsRecordsFormProvider2 @Inject() extends Mappings {

  def apply(): Form[GoodsRecordsFormData] =
    Form(
      mapping(
        "searchText" -> text("goodsRecords.error.required")
          .verifying(maxLength(100, "goodsRecords.error.length")),
        "adviceStatus" -> text("goodsRecords.adviceStatus.error.required")
          .verifying(maxLength(50, "goodsRecords.adviceStatus.error.length")),
        "countryOfOrigin" -> text("goodsRecords.countryOfOrigin.error.required")
          .verifying(maxLength(50, "goodsRecords.countryOfOrigin.error.length"))
      )(GoodsRecordsFormData.apply)(GoodsRecordsFormData.unapply)
    )
}

case class GoodsRecordsFormData(
  searchText: String,
  adviceStatus: String,
  countryOfOrigin: String
)

object GoodsRecordsFormData {
  implicit val format: OFormat[GoodsRecordsFormData] = Json.format[GoodsRecordsFormData]
}


