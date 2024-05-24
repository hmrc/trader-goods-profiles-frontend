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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}

class ExemptionSpec extends AnyFreeSpec with Matchers {

  "certificate" - {

    "must serialise to JSON" in {

      val certificate = Certificate("id", "code", "description")
      val json        = Json.toJson(certificate)

      json mustEqual Json.obj(
        "exemptionType" -> "certificate",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )
    }

    "must deserialise from valid JSON" in {

      val json = Json.obj(
        "exemptionType" -> "certificate",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[Certificate]
      result mustEqual JsSuccess(Certificate("id", "code", "description"))
    }

    "must not deserialise for another exemption type" in {

      val json = Json.obj(
        "exemptionType" -> "additionalCode",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[Certificate]
      result mustBe a[JsError]
    }
  }

  "additional code" - {

    "must serialise to JSON" in {

      val additionalCode = AdditionalCode("id", "code", "description")
      val json           = Json.toJson(additionalCode)

      json mustEqual Json.obj(
        "exemptionType" -> "additionalCode",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )
    }

    "must deserialise from valid JSON" in {

      val json = Json.obj(
        "exemptionType" -> "additionalCode",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[AdditionalCode]
      result mustEqual JsSuccess(AdditionalCode("id", "code", "description"))
    }

    "must not deserialise for another exemption type" in {

      val json = Json.obj(
        "exemptionType" -> "certificate",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[AdditionalCode]
      result mustBe a[JsError]
    }
  }

  "exemption" - {

    "must serialise / deserialise to / from JSON for a certificate" in {

      val json = Json.obj(
        "exemptionType" -> "certificate",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[Exemption]
      result mustEqual JsSuccess(Certificate("id", "code", "description"))
    }

    "must serialise / deserialise to / from JSON for an additional code" in {

      val json = Json.obj(
        "exemptionType" -> "additionalCode",
        "id"            -> "id",
        "code"          -> "code",
        "description"   -> "description"
      )

      val result = json.validate[Exemption]
      result mustEqual JsSuccess(AdditionalCode("id", "code", "description"))
    }
  }
}
