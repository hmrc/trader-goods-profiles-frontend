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
import models.ott.response.CountriesResponse
import play.api.libs.json.*

class CountriesResponseSpec extends SpecBase {

  private val countriesResponse: CountriesResponse =
    CountriesResponse(Seq(Country("GB", "Great Britain"), Country("FR", "France")))
  private val countriesResponseJson: JsObject      =
    Json.obj(
      "data" -> Json.arr(
        Json.obj("attributes" -> Json.obj("id" -> "GB", "description" -> "Great Britain")),
        Json.obj("attributes" -> Json.obj("id" -> "FR", "description" -> "France"))
      )
    )

  "CountriesResponse" - {
    "must deserialize from json" in {
      Json.fromJson[CountriesResponse](countriesResponseJson) mustBe JsSuccess(countriesResponse)
    }

    "must serialize to json" in {
      Json.toJson(countriesResponse) mustBe countriesResponseJson
    }
  }

}
