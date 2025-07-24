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

package models.ott.response

import base.TestConstants.{NiphlCode, NirmsCode}
import cats.implicits.*
import models.ott.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class CategoryAssessmentResponseSpec extends AnyFreeSpec with Matchers {

  val niphlExemption = Certificate("niphlId", NiphlCode, "Some Niphl Description")
  val nirmsExemption = Certificate("nirmsId", "NIRMS", "Some Nirms Description")
  val otherExemption = OtherExemption("otherId", "OTHER", "Other description")

  val validOttResponse = OttResponse(
    goodsNomenclature = GoodsNomenclatureResponse("id", "code", None, Instant.EPOCH, None, List()),
    categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessment1")),
    includedElements = Seq(
      CategoryAssessmentResponse(
        id = "assessment1",
        themeId = "theme1",
        exemptions = Seq(ExemptionResponse("exemption1", models.ott.response.ExemptionType.Certificate)),
        regulationId = "reg1"
      ),
      ThemeResponse(id = "theme1", category = 1, theme = "Theme 1 Description"),
      CertificateResponse(id = "exemption1", code = NiphlCode, description = "NIPHL exemption")
    ),
    descendents = Seq.empty
  )

  "CategoryAssessment.build" - {
    "must return Some(CategoryAssessment) if all data is present" in {
      val result = CategoryAssessment.build("assessment1", validOttResponse)

      result mustBe defined
      val ca = result.get

      ca.id mustBe "assessment1"
      ca.category mustBe 1
      ca.themeDescription mustBe "Theme 1 Description"
      ca.exemptions must have size 1
      ca.exemptions.head.code mustBe NiphlCode

      ca.isCategory1 mustBe true
      ca.isNiphlAnswer mustBe true
      ca.hasExemptions mustBe true
    }

    "must return None if CategoryAssessmentRelationship is missing" in {
      val noMatchResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "code", None, Instant.EPOCH, None, List()),
        categoryAssessmentRelationships = Seq.empty,
        includedElements = Seq.empty,
        descendents = Seq.empty
      )

      val result = CategoryAssessment.build("missing", noMatchResponse)

      result mustBe empty
    }

    "must return Left(errors) if any transformation step fails" in {
      val noMatchResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "code", None, Instant.EPOCH, None, List()),
        categoryAssessmentRelationships = Seq.empty,
        includedElements = Seq.empty,
        descendents = Seq.empty
      )

      val result = CategoryAssessment.build("missing", noMatchResponse)

      result mustBe empty
    }
  }

  "CategoryAssessment instance methods" - {
    "hasExemptions must reflect presence of exemptions" in {
      CategoryAssessment("id1", 1, Seq(niphlExemption), "theme", None).hasExemptions mustBe true
      CategoryAssessment("id2", 1, Nil, "theme", None).hasExemptions mustBe false
    }

    "isNiphlAnswer must be true when category = 1 and contains NIPHL" in {
      CategoryAssessment("id3", 1, Seq(niphlExemption), "theme", None).isNiphlAnswer mustBe true
    }

    "isNiphlAnswer must be false when category != 1 or no NIPHL" in {
      CategoryAssessment("id4", 2, Seq(niphlExemption), "theme", None).isNiphlAnswer mustBe false
      CategoryAssessment("id5", 1, Seq(otherExemption), "theme", None).isNiphlAnswer mustBe false
    }

    "isNirmsAnswer must be true when category = 2 and contains NIRMS" in {
      val ca = CategoryAssessment(
        id = "test-id",
        category = 2,
        exemptions = Seq(Certificate("nirms-id", NirmsCode, "desc")),
        themeDescription = "theme",
        regulationUrl = None
      )

      ca.isNirmsAnswer mustBe true
    }

    "isNirmsAnswer must be false otherwise" in {
      CategoryAssessment("id7", 1, Seq(nirmsExemption), "theme", None).isNirmsAnswer mustBe false
      CategoryAssessment("id8", 2, Seq(otherExemption), "theme", None).isNirmsAnswer mustBe false
    }

    "onlyContainsNiphlAnswer must be true only for NIPHL-only certificates" in {
      val valid     = CategoryAssessment("id", 1, Seq(niphlExemption), "theme", None)
      val mixed     = CategoryAssessment("id", 1, Seq(niphlExemption, otherExemption), "theme", None)
      val wrongType = CategoryAssessment("id", 1, Seq(otherExemption), "theme", None)
      val empty     = CategoryAssessment("id", 1, Nil, "theme", None)

      valid.onlyContainsNiphlAnswer mustBe true
      mixed.onlyContainsNiphlAnswer mustBe false
      wrongType.onlyContainsNiphlAnswer mustBe false
      empty.onlyContainsNiphlAnswer mustBe false
    }
  }

  "needsAnswerEvenIfNoExemptions" - {
    "must return correct result in NIPHL context" in {
      val assessment = CategoryAssessment("id", 1, Seq(niphlExemption), "theme", None)

      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) mustBe false
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "must return correct result when no exemptions and trader NIRMS" in {
      val empty = CategoryAssessment("id", 2, Nil, "theme", None)

      empty.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) mustBe false
      empty.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }
  }

  ".reads" - {
    "must deserialise with no exemptions" in {
      val json = Json.obj(
        "id"            -> "abc",
        "type"          -> "category_assessment",
        "relationships" -> Json.obj(
          "exemptions" -> Json.obj("data" -> Json.arr()),
          "theme"      -> Json.obj("data" -> Json.obj("id" -> "1", "type" -> "theme")),
          "regulation" -> Json.obj("data" -> Json.obj("id" -> "regulationId", "type" -> "legal_act"))
        )
      )

      json.validate[CategoryAssessmentResponse] mustEqual JsSuccess(
        CategoryAssessmentResponse("abc", "1", Nil, "regulationId")
      )
    }

    "must deserialise with all exemption types" in {
      val json = Json.obj(
        "id"            -> "abc",
        "type"          -> "category_assessment",
        "relationships" -> Json.obj(
          "exemptions" -> Json.obj(
            "data" -> Json.arr(
              Json.obj("id" -> "cert", "type"   -> "certificate"),
              Json.obj("id" -> "code", "type"   -> "additional_code"),
              Json.obj("id" -> "exempt", "type" -> "exemption")
            )
          ),
          "theme"      -> Json.obj(
            "data" -> Json.obj(
              "id"   -> "1",
              "type" -> "theme"
            )
          ),
          "regulation" -> Json.obj(
            "data" -> Json.obj(
              "id"   -> "regulationId",
              "type" -> "legal_act"
            )
          )
        )
      )

      val result = json.validate[CategoryAssessmentResponse]

      result mustBe a[JsSuccess[_]]

      val expected = CategoryAssessmentResponse(
        id = "abc",
        themeId = "1",
        exemptions = Seq(
          ExemptionResponse("cert", models.ott.response.ExemptionType.Certificate),
          ExemptionResponse("code", models.ott.response.ExemptionType.AdditionalCode),
          ExemptionResponse("exempt", models.ott.response.ExemptionType.OtherExemption)
        ),
        regulationId = "regulationId"
      )

      result.get mustBe expected
    }
  }
}
