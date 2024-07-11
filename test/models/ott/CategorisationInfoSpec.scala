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

import models.ott.response._
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.Instant

class CategorisationInfoSpec extends AnyFreeSpec with Matchers with OptionValues {

  ".build" - {

    "must return a model from a simple OTT response (one assessment, no exemptions)" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("themeId", 2)
        ),
        descendants = Seq.empty[Descendant]
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment("assessmentId", 2, Nil)
        ),
        Some("some measure unit"),
        0
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must return a model from an OTT response (multiple assessments with exemptions)" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1"),
          CategoryAssessmentRelationship("assessmentId2")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId1", "themeId1", Nil),
          ThemeResponse("themeId1", 1),
          CategoryAssessmentResponse(
            "assessmentId2",
            "themeId2",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
            )
          ),
          ThemeResponse("themeId2", 2),
          CertificateResponse("exemptionId1", "code1", "description1"),
          AdditionalCodeResponse("exemptionId2", "code2", "description2"),
          ThemeResponse("ignoredTheme", 3),
          CertificateResponse("ignoredExemption", "code3", "description3")
        ),
        descendants = Seq.empty[Descendant]
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment("assessmentId1", 1, Nil),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        ),
        Some("some measure unit"),
        0
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must return a model from an OTT response (NIPHLs exemption)" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1"),
          CategoryAssessmentRelationship("assessmentId2")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse(
            "assessmentId1",
            "themeId1",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("WFE012", ExemptionType.OtherExemption)
            )
          ),
          ThemeResponse("themeId1", 1),
          CategoryAssessmentResponse(
            "assessmentId2",
            "themeId2",
            Seq.empty
          ),
          ThemeResponse("themeId2", 2),
          CertificateResponse("exemptionId1", "code1", "description1"),
          AdditionalCodeResponse("exemptionId2", "code2", "description2"),
          OtherExemptionResponse("WFE012", "WFE012", "NIPHLs")
        ),
        descendants = Seq.empty[Descendant]
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment(
            "assessmentId1",
            1,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              OtherExemption("WFE012", "WFE012", "NIPHLs")
            )
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq.empty
          )
        ),
        Some("some measure unit"),
        0
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must order its assessments in order of category (lowest first) then number of exemptions (lowest first)" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1"),
          CategoryAssessmentRelationship("assessmentId2"),
          CategoryAssessmentRelationship("assessmentId3"),
          CategoryAssessmentRelationship("assessmentId4")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse(
            "assessmentId1",
            "themeId1",
            Seq(ExemptionResponse("exemptionId1", ExemptionType.Certificate))
          ),
          CategoryAssessmentResponse(
            "assessmentId2",
            "themeId2",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
            )
          ),
          CategoryAssessmentResponse(
            "assessmentId3",
            "themeId1",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
            )
          ),
          CategoryAssessmentResponse(
            "assessmentId4",
            "themeId2",
            Nil
          ),
          ThemeResponse("themeId1", 1),
          ThemeResponse("themeId2", 2),
          CertificateResponse("exemptionId1", "code1", "description1"),
          AdditionalCodeResponse("exemptionId2", "code2", "description2"),
          ThemeResponse("ignoredTheme", 3),
          CertificateResponse("ignoredExemption", "code3", "description3")
        ),
        descendants = Seq(Descendant("1", "type1"), Descendant("2", "type2"))
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment(
            "assessmentId1",
            1,
            Seq(Certificate("exemptionId1", "code1", "description1"))
          ),
          CategoryAssessment(
            "assessmentId3",
            1,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          ),
          CategoryAssessment(
            "assessmentId4",
            2,
            Nil
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        ),
        Some("some measure unit"),
        2
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must return None when a category assessment cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          ThemeResponse("otherThemeId", 2)
        ),
        descendants = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when the correct theme cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("otherThemeId", 2)
        ),
        descendants = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when a certificate cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse(
            "assessmentId1",
            "themeId1",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
            )
          ),
          ThemeResponse("themeId1", 1),
          AdditionalCodeResponse("exemptionId2", "code2", "description2")
        ),
        descendants = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when an additional code cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature =
          GoodsNomenclatureResponse("id", "commodity code", Some("some measure unit"), Instant.EPOCH, None, "test"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse(
            "assessmentId1",
            "themeId1",
            Seq(
              ExemptionResponse("exemptionId1", ExemptionType.Certificate),
              ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
            )
          ),
          ThemeResponse("themeId1", 1),
          CertificateResponse("exemptionId1", "code1", "description1")
        ),
        descendants = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }
  }
}
