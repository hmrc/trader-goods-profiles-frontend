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

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, seq, text}
import play.api.libs.json.{Format, Json}

case class SearchForm(searchTerm: Option[String], countryOfOrigin: Option[String], statusValue: Seq[String] = Seq())

object SearchForm {
  implicit val format: Format[SearchForm] = Json.format[SearchForm]

  val form: Form[SearchForm] = Form(
    mapping(
      "searchTerm"      -> optional(text).transform[Option[String]](_.map(_.trim), identity),
      "countryOfOrigin" -> optional(text),
      "statusValue"     -> seq(text)
    )(SearchForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )
}
