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

package models.ott.response

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExemptionResponseSpec extends AnyFreeSpec with Matchers {

  ".reads" - {

    "must deserialise valid JSON for a certificate" in {

      val json = Json.obj(
        "id"   -> "1",
        "type" -> "certificate"
      )

      val result = json.validate[ExemptionResponse]
      result mustEqual JsSuccess(ExemptionResponse("1", ExemptionType.Certificate))
    }

    "must deserialise valid JSON for an additional code" in {

      val json = Json.obj(
        "id"   -> "1",
        "type" -> "additional_code"
      )

      val result = json.validate[ExemptionResponse]
      result mustEqual JsSuccess(ExemptionResponse("1", ExemptionType.AdditionalCode))
    }

    "must fail to deserialise for any other type" in {

      val json = Json.obj(
        "id"   -> "1",
        "type" -> "foo"
      )

      val result = json.validate[ExemptionResponse]
      result mustEqual JsError(JsPath \ "type", "unable to parse exemption type")
    }
  }
}
