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

class EmailSpec extends AnyFreeSpec with Matchers {

  "Email JSON serialization" - {

    val timestamp = Instant.parse("2025-08-05T15:30:00Z")
    val email     = Email("user@example.com", timestamp)

    val json = Json.obj(
      "address"   -> "user@example.com",
      "timestamp" -> "2025-08-05T15:30:00Z"
    )

    "must serialize to JSON" in {
      Json.toJson(email) mustBe json
    }

    "must deserialize from JSON" in {
      Json.fromJson[Email](json) mustBe JsSuccess(email)
    }
  }
}
