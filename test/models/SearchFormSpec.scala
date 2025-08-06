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

import base.SpecBase
import play.api.data.Form
import play.api.libs.json._

class SearchFormSpec extends SpecBase {
  "SearchForm" - {

    "bind from data correctly" in {
      val validData = Map(
        "searchTerm"      -> "apple",
        "countryOfOrigin" -> "US",
        "statusValue[0]"  -> "active",
        "statusValue[1]"  -> "inactive"
      )

      val form: Form[SearchForm] = SearchForm.form.bind(validData)

      form.hasErrors mustBe false
      form.get.searchTerm mustBe Some("apple")
      form.get.countryOfOrigin mustBe Some("US")
      form.get.statusValue mustBe Seq("active", "inactive")
    }

    "bind from data with missing optional fields" in {
      val validData = Map("statusValue[0]" -> "active")

      val form: Form[SearchForm] = SearchForm.form.bind(validData)

      form.hasErrors mustBe false
      form.get.searchTerm mustBe None
      form.get.countryOfOrigin mustBe None
      form.get.statusValue mustBe Seq("active")
    }
  }

  "SearchForm JSON format" - {

    "serialize to JSON" in {
      val form = SearchForm(Some("apple"), Some("US"), Seq("active", "inactive"))
      val json = Json.toJson(form)

      (json \ "searchTerm").as[String] mustBe "apple"
      (json \ "countryOfOrigin").as[String] mustBe "US"
      (json \ "statusValue").as[Seq[String]] mustBe Seq("active", "inactive")
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "searchTerm"      -> "apple",
        "countryOfOrigin" -> "US",
        "statusValue"     -> Seq("active", "inactive")
      )

      val form = json.as[SearchForm]

      form.statusValue mustBe Seq("active", "inactive")
    }

    "deserialize from JSON with missing optional fields" in {
      val json = Json.obj(
        "statusValue" -> Seq("active")
      )

      val form = json.as[SearchForm]

      form.statusValue mustBe Seq("active")
    }
  }
}
