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

import base.SpecBase
import base.TestConstants.testRecordId
import models.ott.response._
import models.{AssessmentAnswer, RecordCategorisations}
import pages.AssessmentPage
import queries.RecordCategorisationsQuery

import java.time.Instant

class CategorisationInfoSpec extends SpecBase {

  ".build" - {

    "must return a model from a simple OTT response (one assessment, no exemptions)" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("themeId", 2)
        ),
        descendents = Seq.empty[Descendant]
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
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
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
        descendents = Seq.empty[Descendant]
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

    "must order its assessments in order of category (lowest first) then number of exemptions (lowest first)" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
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
        descendents = Seq(Descendant("1", "type1"), Descendant("2", "type2"))
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
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          ThemeResponse("otherThemeId", 2)
        ),
        descendents = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when the correct theme cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
        categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
        includedElements = Seq(
          CategoryAssessmentResponse("assessmentId", "themeId", Nil),
          ThemeResponse("otherThemeId", 2)
        ),
        descendents = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when a certificate cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
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
        descendents = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }

    "must return None when an additional code cannot be found" in {

      val ottResponse = OttResponse(
        goodsNomenclature = GoodsNomenclatureResponse(
          "id",
          "commodity code",
          Some("some measure unit"),
          Instant.EPOCH,
          None,
          List("test")
        ),
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
        descendents = Seq.empty[Descendant]
      )

      CategorisationInfo.build(ottResponse) must not be defined
    }
  }

  "areThereAnyNonAnsweredQuestions" - {

    val categorisationInfo = CategorisationInfo(
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

    "return true if non-answered questions" in {
      val userAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfo)))
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("true"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("true"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NotAnsweredYet)
        .success
        .value
        .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.NoExemption)
        .success
        .value

      categorisationInfo.areThereAnyNonAnsweredQuestions(testRecordId, userAnswers) mustBe true
    }

    "return false if all questions answered or do not need to be answered" in {
      val userAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfo)))
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("true"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("true"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
        .success
        .value

      categorisationInfo.areThereAnyNonAnsweredQuestions(testRecordId, userAnswers) mustBe false
    }
  }
}
