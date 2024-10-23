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
import play.api.libs.json.{JsSuccess, Json}

class IncludedElementSpec extends AnyFreeSpec with Matchers {

  ".reads" - {

    "must deserialise a category assessment" in {

      val json = Json.obj(
        "id"            -> "abc",
        "type"          -> "category_assessment",
        "relationships" -> Json.obj(
          "exemptions" -> Json.obj(
            "data" -> Json.arr(
              Json.obj(
                "id"   -> "cert",
                "type" -> "certificate"
              ),
              Json.obj(
                "id"   -> "code",
                "type" -> "additional_code"
              )
            )
          ),
          "theme"      -> Json.obj(
            "data" -> Json.obj(
              "id"   -> "1",
              "type" -> "theme"
            )
          )
        )
      )

      val result = json.validate[IncludedElement]
      result mustEqual JsSuccess(
        CategoryAssessmentResponse(
          id = "abc",
          themeId = "1",
          exemptions = Seq(
            ExemptionResponse("cert", ExemptionType.Certificate),
            ExemptionResponse("code", ExemptionType.AdditionalCode)
          )
        )
      )
    }

    "must deserialise a certificate" in {

      val json = Json.obj(
        "type"       -> "certificate",
        "id"         -> "1",
        "attributes" -> Json.obj(
          "code"        -> "abc",
          "description" -> "foo"
        )
      )

      val result = json.validate[IncludedElement]
      result mustEqual JsSuccess(CertificateResponse("1", "abc", "foo"))
    }

    "must deserialise an additional code" in {

      val json = Json.obj(
        "type"       -> "additional_code",
        "id"         -> "1",
        "attributes" -> Json.obj(
          "code"        -> "abc",
          "description" -> "foo"
        )
      )

      val result = json.validate[IncludedElement]
      result mustEqual JsSuccess(AdditionalCodeResponse("1", "abc", "foo"))
    }

    "must deserialise a theme" in {

      val json = Json.obj(
        "type"       -> "theme",
        "id"         -> "1",
        "attributes" -> Json.obj(
          "category" -> 1,
          "theme"    -> "theme description"
        )
      )

      val result = json.validate[IncludedElement]
      result mustEqual JsSuccess(ThemeResponse("1", 1, "theme description"))
    }

    "must deserialise other types as Ignorable" in {

      Json.obj("type" -> "foo").validate[IncludedElement] mustEqual JsSuccess(Ignorable())
    }
  }
}
