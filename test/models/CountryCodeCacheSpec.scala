/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class CountryCodeCacheSpec extends AnyFreeSpec with Matchers {

  "CountryCodeCache JSON serialization" - {

    val instant = Instant.parse("2025-08-05T12:00:00Z")

    val countries = Seq(
      Country("GB", "United Kingdom"),
      Country("FR", "France")
    )

    val cache = CountryCodeCache(
      key = "test-key",
      data = countries,
      lastUpdated = instant
    )

    val json = Json.obj(
      "key"         -> "test-key",
      "data"        -> Json.arr(
        Json.obj("attributes" -> Json.obj("id" -> "GB", "description" -> "United Kingdom")),
        Json.obj("attributes" -> Json.obj("id" -> "FR", "description" -> "France"))
      ),
      "lastUpdated" -> "2025-08-05T12:00:00Z"
    )

    "must serialize to JSON" in {
      Json.toJson(cache) mustBe json
    }

    "must deserialize from JSON" in {
      Json.fromJson[CountryCodeCache](json) mustBe JsSuccess(cache)
    }
  }
}
