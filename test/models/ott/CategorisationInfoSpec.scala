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

class CategorisationInfoSpec extends AnyFreeSpec with Matchers with OptionValues {

  ".build" - {

    "must return a model from a simple OTT response (one assessment, no exemptions)" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("themeId", 2)
        )
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment("assessmentId", 2, Nil)
        )
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must return a model from an OTT response (multiple assessments with exemptions)" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1"),
          CategoryAssessmentRelationship("assessmentId2")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId1", "themeId1", Nil),
          ThemeResponse("themeId1", 1),
          CategoryAssessmentResponse("assessmentId2", "themeId2", Seq(
            ExemptionResponse("exemptionId1", "certificate"),
            ExemptionResponse("exemptionId2", "certificate")
          )),
          ThemeResponse("themeId2", 2),
          CertificateResponse("exemptionId1", "code1", "description1"),
          CertificateResponse("exemptionId2", "code2", "description2"),
          ThemeResponse("ignoredTheme", 3),
          CertificateResponse("ignoredExemption", "code3", "description3")
        )
      )

      val expectedResult = CategorisationInfo(
        commodityCode = "commodity code",
        categoryAssessments = Seq(
          CategoryAssessment("assessmentId1", 1, Nil),
          CategoryAssessment("assessmentId2", 2, Seq(
            Exemption("exemptionId1", "code1", "description1"),
            Exemption("exemptionId2", "code2", "description2")
          ))
        )
      )

      val result = CategorisationInfo.build(ottResponse)
      result.value mustEqual expectedResult
    }

    "must return None when a category assessment cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          ThemeResponse("otherThemeId", 2)
        )
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when the correct theme cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code"),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("otherThemeId", 2)
        )
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when an exemption cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code"),
        categoryAssessmentRelationships = Seq(
          CategoryAssessmentRelationship("assessmentId1")
        ),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId1", "themeId1", Seq(
            ExemptionResponse("exemptionId1", "certificate"),
            ExemptionResponse("exemptionId2", "certificate")
          )),
          ThemeResponse("themeId1", 1),
          CertificateResponse("exemptionId1", "code1", "description1"),
        )
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }
  }
}
