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

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import viewmodels.govuk.select._

final case class Country(
  id: String,
  description: String
)

object Country {

  def selectItems(countries: Seq[Country])(implicit messages: Messages): Seq[SelectItem] =
    SelectItem(value = None, text = messages("countryOfOrigin.selectCountry")) +:
      countries.map { data =>
        SelectItemViewModel(
          value = data.id,
          text = data.description
        )
      }

  val reads: Reads[Country] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "attributes" \ "id").read[String] and
        (__ \ "attributes" \ "description").read[String]
    )(Country.apply _)
  }

  val writes: OWrites[Country] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "attributes" \ "id").write[String] and
        (__ \ "attributes" \ "description").write[String]
    )(unlift(Country.unapply))
  }

  implicit val format: OFormat[Country] = OFormat(reads, writes)
}
