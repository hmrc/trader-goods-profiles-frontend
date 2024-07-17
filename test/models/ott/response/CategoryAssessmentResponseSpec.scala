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

import models.ott.{CategoryAssessment, Certificate, OtherExemption}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class CategoryAssessmentResponseSpec extends AnyFreeSpec with Matchers {

  ".reads" - {

    "must deserialise valid JSON with no exemptions" in {

      val json = Json.obj(
        "id"            -> "abc",
        "type"          -> "category_assessment",
        "relationships" -> Json.obj(
          "exemptions" -> Json.obj(
            "data" -> Json.arr()
          ),
          "theme"      -> Json.obj(
            "data" -> Json.obj(
              "id"   -> "1",
              "type" -> "theme"
            )
          )
        )
      )

      val result = json.validate[CategoryAssessmentResponse]
      result mustEqual JsSuccess(CategoryAssessmentResponse("abc", "1", Nil))
    }

    "must deserialise valid JSON with exemptions" in {

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
              ),
              Json.obj(
                "id"   -> "exempt",
                "type" -> "exemption"
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

      val result = json.validate[CategoryAssessmentResponse]
      result mustEqual JsSuccess(
        CategoryAssessmentResponse(
          id = "abc",
          themeId = "1",
          exemptions = Seq(
            ExemptionResponse("cert", ExemptionType.Certificate),
            ExemptionResponse("code", ExemptionType.AdditionalCode),
            ExemptionResponse("exempt", ExemptionType.OtherExemption)
          )
        )
      )
    }

    "must deserialise valid JSON with exemptions including NIPHLs" in {

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
              ),
              Json.obj(
                "id"   -> "WFE013",
                "type" -> "exemption"
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

      val result = json.validate[CategoryAssessmentResponse]
      result mustEqual JsSuccess(
        CategoryAssessmentResponse(
          id = "abc",
          themeId = "1",
          exemptions = Seq(
            ExemptionResponse("cert", ExemptionType.Certificate),
            ExemptionResponse("code", ExemptionType.AdditionalCode),
            ExemptionResponse("WFE013", ExemptionType.OtherExemption)
          )
        )
      )

    }

  }

}
