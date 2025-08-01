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
import base.TestConstants.{NiphlCode, NirmsCode, testRecordId}
import models.ott.response.*
import models._
import org.scalatest.matchers.should.Matchers.shouldBe
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}

import java.time.Instant

class CategorisationInfoSpec extends SpecBase {

  private val testTraderProfileResponseWithoutNiphlAndNirms: TraderProfile =
    TraderProfile("actorId", "ukims number", None, None, eoriChanged = false)

  "CategorisationInfo#isAutoCategorisable" - {

    "return true when at least one CategoryAssessment has no exemptions" in {
      val assessmentWithoutExemptions = CategoryAssessment(
        id = "a1",
        category = 1,
        exemptions = Seq.empty,
        themeDescription = "Some theme",
        regulationUrl = None
      )

      val info = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(assessmentWithoutExemptions),
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      info.isAutoCategorisable shouldBe true
    }

    "return false when all CategoryAssessments have at least one exemption" in {
      val certificate = Certificate("cert-id", "cert-code", "cert-description")

      val assessmentWithExemption = CategoryAssessment(
        id = "a2",
        category = 1,
        exemptions = Seq(certificate),
        themeDescription = "Another theme",
        regulationUrl = None
      )

      val info = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(assessmentWithExemption),
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      info.isAutoCategorisable shouldBe false
    }

    "return false when there are no CategoryAssessments" in {
      val info = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq.empty,
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      info.isAutoCategorisable shouldBe false
    }
  }

  ".build" - {

    "must return a model from an OTT response" - {

      "one assessment with exemptions" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234567890",
            Some("some measure unit"),
            validityStartDate,
            Some(validityEndDate),
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          countryOfOrigin = "BV",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          Some("some measure unit"),
          0
        )

        val result =
          CategorisationInfo.build(ottResponse, "BV", "1234567890", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult
      }

      "and order its assessments in order of category (lowest first) then number of exemptions (lowest first)" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234567890",
            Some("some measure unit"),
            validityStartDate,
            Some(validityEndDate),
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
              Seq(ExemptionResponse("exemptionId1", ExemptionType.Certificate)),
              "regulationId1"
            ),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId2"
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId1",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            CategoryAssessmentResponse(
              "assessmentId4",
              "themeId2",
              Seq(ExemptionResponse("exemptionId1", ExemptionType.Certificate)),
              "regulationId2"
            ),
            ThemeResponse("themeId1", 1, "theme description"),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
            LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2"))
          ),
          descendents = Seq(Descendant("1", "type1"), Descendant("2", "type2"))
        )

        val expectedAssessments = Seq(
          CategoryAssessment(
            "assessmentId1",
            1,
            Seq(Certificate("exemptionId1", "code1", "description1")),
            "theme description",
            Some("regulationUrl1")
          ),
          CategoryAssessment(
            "assessmentId3",
            1,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          ),
          CategoryAssessment(
            "assessmentId4",
            2,
            Seq(Certificate("exemptionId1", "code1", "description1")),
            "theme description",
            Some("regulationUrl2")
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl2")
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          countryOfOrigin = "BV",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = expectedAssessments,
          categoryAssessmentsThatNeedAnswers = expectedAssessments,
          Some("some measure unit"),
          2
        )

        val result =
          CategorisationInfo.build(ottResponse, "BV", "1234567890", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult
      }

      "with the shorter entered commodity code rather than the padded ott one" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234560000",
            None,
            validityStartDate,
            Some(validityEndDate),
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "123456",
          countryOfOrigin = "BV",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          None,
          0
        )

        val result =
          CategorisationInfo.build(ottResponse, "BV", "123456", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult
      }

      "when there are no assessments against the commodity" in {

        val ottResponseNoAssessments = OttResponse(
          GoodsNomenclatureResponse(
            "some id",
            "1234567890",
            None,
            validityStartDate,
            None,
            List("test")
          ),
          Seq[CategoryAssessmentRelationship](),
          Seq[IncludedElement](),
          Seq[Descendant]()
        )

        val expectedResult = CategorisationInfo("1234567890", "BV", None, Seq.empty, Seq.empty, None, 0)

        val result =
          CategorisationInfo.build(
            ottResponseNoAssessments,
            "BV",
            "1234567890",
            testTraderProfileResponseWithoutNiphlAndNirms
          )
        result.value mustEqual expectedResult
      }

      "when there is a Category 1 assessment with no exemptions" in {
        val mockOttResponse = OttResponse(
          GoodsNomenclatureResponse(
            "some id",
            "1234567890",
            None,
            Instant.EPOCH,
            None,
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2"),
            CategoryAssessmentRelationship("assessmentId1"),
            CategoryAssessmentRelationship("assessmentId3")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId1",
              Seq.empty,
              "regulationId1"
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId1",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)),
              "regulationId2"
            ),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
            LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessments = Seq(
          CategoryAssessment("assessmentId1", 1, Seq.empty, "theme description", Some("regulationUrl1")),
          CategoryAssessment(
            "assessmentId3",
            1,
            Seq(AdditionalCode("exemptionId2", "code2", "description2")),
            "theme description",
            Some("regulationUrl2")
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          )
        )

        val expectedResult =
          CategorisationInfo("1234567890", "BV", None, expectedAssessments, Seq.empty, None, 0)

        val result =
          CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult

      }

      "when there are no Category 1 and there are Category 2 assessment with no exemptions" in {
        val mockOttResponse = OttResponse(
          GoodsNomenclatureResponse(
            "some id",
            "1234567890",
            None,
            Instant.EPOCH,
            None,
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2"),
            CategoryAssessmentRelationship("assessmentId1"),
            CategoryAssessmentRelationship("assessmentId3")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId2",
              Seq.empty,
              "regulationId1"
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId2",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)),
              "regulationId2"
            ),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
            LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description1"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessments = Seq(
          CategoryAssessment("assessmentId1", 2, Seq.empty, "theme description", Some("regulationUrl1")),
          CategoryAssessment(
            "assessmentId3",
            2,
            Seq(AdditionalCode("exemptionId2", "code2", "description2")),
            "theme description",
            Some("regulationUrl2")
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          )
        )

        val expectedResult =
          CategorisationInfo("1234567890", "BV", None, expectedAssessments, Seq.empty, None, 0)

        val result =
          CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult

      }

      "when there are Category 1 that need answers but there are Category 2 assessment with no exemptions" in {
        val mockOttResponse = OttResponse(
          GoodsNomenclatureResponse(
            "some id",
            "1234567890",
            None,
            validityStartDate,
            Some(validityEndDate),
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2"),
            CategoryAssessmentRelationship("assessmentId1"),
            CategoryAssessmentRelationship("assessmentId3")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId1",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId2",
              Seq.empty,
              "regulationId1"
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId1",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)),
              "regulationId2"
            ),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
            LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description1"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessmentId1 =
          CategoryAssessment("assessmentId1", 2, Seq.empty, "theme description", Some("regulationUrl1"))
        val expectedAssesmentId2  = CategoryAssessment(
          "assessmentId2",
          1,
          Seq(
            Certificate("exemptionId1", "code1", "description1"),
            AdditionalCode("exemptionId2", "code2", "description2")
          ),
          "theme description",
          Some("regulationUrl1")
        )
        val expectedAssessmentId3 = CategoryAssessment(
          "assessmentId3",
          1,
          Seq(AdditionalCode("exemptionId2", "code2", "description2")),
          "theme description",
          Some("regulationUrl2")
        )

        val expectedAssessments = Seq(
          expectedAssessmentId3,
          expectedAssesmentId2,
          expectedAssessmentId1
        )

        val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId3, expectedAssesmentId2)

        val expectedResult =
          CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            expectedAssessments,
            expectedAssessmentsThatNeedAnswers,
            None,
            0
          )

        val result =
          CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithoutNiphlAndNirms)
        result.value mustEqual expectedResult
      }

      "when flagged as longer code" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234567890",
            Some("some measure unit"),
            Instant.EPOCH,
            None,
            List("test")
          ),
          categoryAssessmentRelationships = Seq(
            CategoryAssessmentRelationship("assessmentId2")
          ),
          includedElements = Seq(
            ThemeResponse("themeId1", 1, "theme description"),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId2",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId2", 2, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3, "theme description"),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
          ),
          descendents = Seq.empty[Descendant]
        )

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            ),
            "theme description",
            Some("regulationUrl1")
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          "BV",
          comcodeEffectiveToDate = None,
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          Some("some measure unit"),
          0,
          longerCode = true
        )

        val result =
          CategorisationInfo.build(
            ottResponse,
            "BV",
            "1234567890",
            testTraderProfileResponseWithoutNiphlAndNirms,
            longerCode = true
          )
        result.value mustEqual expectedResult
      }

      "when NIPHL" - {

        ".is authorised" - {

          val testTraderProfileResponseWithNiphlAndNirms =
            TraderProfile("actorId", "ukims number", Some("nirms number"), Some("niphl number"), eoriChanged = false)

          "when there is a Category 1 assessment that need answers and there are Category 2 assessment with no exemptions" in {
            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId1")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId2"
                ),
                ThemeResponse("themeId2", 2, "theme description"),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("ignoredTheme", 3, "theme description"),
                CertificateResponse("ignoredExemption", "code3", "description3"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId2",
                  Seq.empty,
                  "regulationId1"
                ),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1 =
              CategoryAssessment("assessmentId1", 2, Seq.empty, "theme description", Some("regulationUrl1"))
            val expectedAssesmentId2  = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl2")
            )

            val expectedAssessments = Seq(
              expectedAssesmentId2,
              expectedAssessmentId1
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssesmentId2)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNiphlAuthorised = true,
                isTraderNirmsAuthorised = true
              )

            val result =
              CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithNiphlAndNirms)
            result.value mustEqual expectedResult
          }

          "when there is a Category 1 assessment that need answers and there is a Category 2 assessment with no exemptions but there is a NIPHL assessment" in {
            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NiphlCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NiphlCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty,
                  "regulationId2"
                ),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description1"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNiphlAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate(NiphlCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     =
              CategoryAssessment("assessmentId3", 2, Seq.empty, "theme description", Some("regulationUrl2"))

            val expectedAssessments = Seq(
              expectedNiphlAssesmentId2,
              expectedAssessmentId1,
              expectedAssessmentId3
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNiphlAuthorised = true,
                isTraderNirmsAuthorised = true
              )

            val result =
              CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithNiphlAndNirms)
            result.value mustEqual expectedResult
          }
        }

        ".is unauthorised" - {

          "when there is a Category 1 assessment and there is a Category 2 assessment with no exemptions but there is a NIPHL assessment" in {
            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NiphlCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NiphlCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty,
                  "regulationId3"
                ),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2")),
                LegalActResponse(Some("regulationId3"), Some("regulationUrl3"), Some("description3"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNiphlAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate(NiphlCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     =
              CategoryAssessment("assessmentId3", 2, Seq.empty, "theme description", Some("regulationUrl3"))

            val expectedAssessments = Seq(
              expectedNiphlAssesmentId2,
              expectedAssessmentId1,
              expectedAssessmentId3
            )

            val expectedAssessmentsThatNeedAnswers = Seq.empty

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0
              )

            val result =
              CategorisationInfo.build(
                mockOttResponse,
                "BV",
                "1234567890",
                testTraderProfileResponseWithoutNiphlAndNirms
              )

            result.value mustEqual expectedResult
          }
        }
      }

      "when there is a" - {
        "NIPHL assessment with other possible exemptions and trader is not authorised" in {
          val mockOttResponse = OttResponse(
            GoodsNomenclatureResponse(
              "some id",
              "1234567890",
              None,
              Instant.EPOCH,
              None,
              List("test")
            ),
            categoryAssessmentRelationships = Seq(
              CategoryAssessmentRelationship("assessmentId1")
            ),
            includedElements = Seq(
              ThemeResponse("themeId1", 1, "theme description"),
              CertificateResponse("exemptionId1", "code1", "description1"),
              CategoryAssessmentResponse(
                "assessmentId1",
                "themeId1",
                Seq(
                  ExemptionResponse(NiphlCode, ExemptionType.Certificate),
                  ExemptionResponse("exemptionId1", ExemptionType.Certificate)
                ),
                "regulationId1"
              ),
              CertificateResponse(NiphlCode, "WFE-code", "WFE-description"),
              CertificateResponse("exemptionId1", "code1", "description1"),
              LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
            ),
            descendents = Seq.empty[Descendant]
          )

          val expectedNiphlAssessment = CategoryAssessment(
            "assessmentId1",
            1,
            Seq(
              Certificate(NiphlCode, "WFE-code", "WFE-description"),
              Certificate("exemptionId1", "code1", "description1")
            ),
            "theme description",
            Some("regulationUrl1")
          )

          val expectedAssessments = Seq(
            expectedNiphlAssessment
          )

          val expectedAssessmentsThatNeedAnswers = Seq(expectedNiphlAssessment)

          val expectedResult =
            CategorisationInfo(
              "1234567890",
              "BV",
              None,
              expectedAssessments,
              expectedAssessmentsThatNeedAnswers,
              None,
              0
            )

          val result =
            CategorisationInfo.build(
              mockOttResponse,
              "BV",
              "1234567890",
              testTraderProfileResponseWithoutNiphlAndNirms
            )

          result.value mustEqual expectedResult

        }
        "NRIMS assessment with other possible exemptions and trader is not authorised" in {
          val mockOttResponse = OttResponse(
            GoodsNomenclatureResponse(
              "some id",
              "1234567890",
              None,
              Instant.EPOCH,
              None,
              List("test")
            ),
            categoryAssessmentRelationships = Seq(
              CategoryAssessmentRelationship("assessmentId1")
            ),
            includedElements = Seq(
              ThemeResponse("themeId1", 2, "theme description"),
              CertificateResponse("exemptionId1", "code1", "description1"),
              CategoryAssessmentResponse(
                "assessmentId1",
                "themeId1",
                Seq(
                  ExemptionResponse(NirmsCode, ExemptionType.Certificate),
                  ExemptionResponse("exemptionId1", ExemptionType.Certificate)
                ),
                "regulationId1"
              ),
              CertificateResponse(NirmsCode, "WFE-code", "WFE-description"),
              CertificateResponse("exemptionId1", "code1", "description1"),
              LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1"))
            ),
            descendents = Seq.empty[Descendant]
          )

          val expectedNirmsAssesmentId1 = CategoryAssessment(
            "assessmentId1",
            2,
            Seq(
              Certificate(NirmsCode, "WFE-code", "WFE-description"),
              Certificate("exemptionId1", "code1", "description1")
            ),
            "theme description",
            Some("regulationUrl1")
          )

          val expectedAssessments = Seq(
            expectedNirmsAssesmentId1
          )

          val expectedAssessmentsThatNeedAnswers = Seq(expectedNirmsAssesmentId1)

          val expectedResult =
            CategorisationInfo(
              "1234567890",
              "BV",
              None,
              expectedAssessments,
              expectedAssessmentsThatNeedAnswers,
              None,
              0
            )

          val result =
            CategorisationInfo.build(
              mockOttResponse,
              "BV",
              "1234567890",
              testTraderProfileResponseWithoutNiphlAndNirms
            )

          result.value mustEqual expectedResult

        }

      }

      "when there is a NIRMS assessment" - {

        "and NIRMS not authorised" - {
          "with Category 1 assessment and there is a Category 2 assessment" in {

            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NirmsCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NirmsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq(
                    ExemptionResponse("exemptionId3", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId4", ExemptionType.AdditionalCode)
                  ),
                  "regulationId3"
                ),
                CertificateResponse("exemptionId3", "code3", "description3"),
                AdditionalCodeResponse("exemptionId4", "code4", "description4"),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2")),
                LegalActResponse(Some("regulationId3"), Some("regulationUrl3"), Some("description3"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNirmsAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              2,
              Seq(
                Certificate(NirmsCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     = CategoryAssessment(
              "assessmentId3",
              2,
              Seq(
                Certificate("exemptionId3", "code3", "description3"),
                AdditionalCode("exemptionId4", "code4", "description4")
              ),
              "theme description",
              Some("regulationUrl3")
            )

            val expectedAssessments = Seq(
              expectedAssessmentId1,
              expectedNirmsAssesmentId2,
              expectedAssessmentId3
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0
              )

            val result =
              CategorisationInfo.build(
                mockOttResponse,
                "BV",
                "1234567890",
                testTraderProfileResponseWithoutNiphlAndNirms
              )

            result.value mustEqual expectedResult

          }

          "with Category 1 assessment and there is a Category 2 assessment with no exemptions" in {

            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NirmsCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NirmsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty,
                  "regulationId3"
                ),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2")),
                LegalActResponse(Some("regulationId3"), Some("regulationUrl3"), Some("description3"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNirmsAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              2,
              Seq(
                Certificate(NirmsCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     =
              CategoryAssessment("assessmentId3", 2, Seq.empty, "theme description", Some("regulationUrl3"))

            val expectedAssessments = Seq(
              expectedAssessmentId1,
              expectedAssessmentId3,
              expectedNirmsAssesmentId2
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0
              )

            val result =
              CategorisationInfo.build(
                mockOttResponse,
                "BV",
                "1234567890",
                testTraderProfileResponseWithoutNiphlAndNirms
              )
            result.value mustEqual expectedResult
          }
        }

        "and NIRMS authorised" - {

          val testTraderProfileResponseWithNirms =
            TraderProfile("actorId", "ukims number", Some("nirms number"), None, eoriChanged = false)

          "with Category 1 assessment and there is a Category 2 assessment" in {

            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NirmsCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NirmsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq(
                    ExemptionResponse("exemptionId3", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId4", ExemptionType.AdditionalCode)
                  ),
                  "regulationId3"
                ),
                CertificateResponse("exemptionId3", "code3", "description3"),
                AdditionalCodeResponse("exemptionId4", "code4", "description4"),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2")),
                LegalActResponse(Some("regulationId3"), Some("regulationUrl3"), Some("description3"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNirmsAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              2,
              Seq(
                Certificate(NirmsCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     = CategoryAssessment(
              "assessmentId3",
              2,
              Seq(
                Certificate("exemptionId3", "code3", "description3"),
                AdditionalCode("exemptionId4", "code4", "description4")
              ),
              "theme description",
              Some("regulationUrl3")
            )

            val expectedAssessments = Seq(
              expectedAssessmentId1,
              expectedNirmsAssesmentId2,
              expectedAssessmentId3
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1, expectedAssessmentId3)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNirmsAuthorised = true
              )

            val result =
              CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithNirms)
            result.value mustEqual expectedResult
          }

          "with Category 1 assessment and there is a Category 2 assessment with no exemptions" in {

            val mockOttResponse = OttResponse(
              GoodsNomenclatureResponse(
                "some id",
                "1234567890",
                None,
                Instant.EPOCH,
                None,
                List("test")
              ),
              categoryAssessmentRelationships = Seq(
                CategoryAssessmentRelationship("assessmentId1"),
                CategoryAssessmentRelationship("assessmentId2"),
                CategoryAssessmentRelationship("assessmentId3")
              ),
              includedElements = Seq(
                ThemeResponse("themeId1", 1, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  ),
                  "regulationId1"
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NirmsCode, ExemptionType.Certificate)
                  ),
                  "regulationId2"
                ),
                CertificateResponse(NirmsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2, "theme description"),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty,
                  "regulationId3"
                ),
                LegalActResponse(Some("regulationId1"), Some("regulationUrl1"), Some("description1")),
                LegalActResponse(Some("regulationId2"), Some("regulationUrl2"), Some("description2")),
                LegalActResponse(Some("regulationId3"), Some("regulationUrl3"), Some("description3"))
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              ),
              "theme description",
              Some("regulationUrl1")
            )
            val expectedNirmsAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              2,
              Seq(
                Certificate(NirmsCode, "WFE-code", "WFE-description")
              ),
              "theme description",
              Some("regulationUrl2")
            )
            val expectedAssessmentId3     =
              CategoryAssessment("assessmentId3", 2, Seq.empty, "theme description", Some("regulationUrl3"))

            val expectedAssessments = Seq(
              expectedAssessmentId1,
              expectedAssessmentId3,
              expectedNirmsAssesmentId2
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                "BV",
                None,
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNirmsAuthorised = true
              )

            val result =
              CategorisationInfo.build(mockOttResponse, "BV", "1234567890", testTraderProfileResponseWithNirms)
            result.value mustEqual expectedResult
          }
        }

      }
    }

    "must return None" - {
      "when a category assessment cannot be found" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234567890",
            Some("some measure unit"),
            Instant.EPOCH,
            None,
            List("test")
          ),
          categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
          includedElements = Seq(
            ThemeResponse("otherThemeId", 2, "theme description")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(
          ottResponse,
          "BV",
          "1234567890",
          testTraderProfileResponseWithoutNiphlAndNirms
        ) mustBe None
      }

      "when the correct theme cannot be found" in {

        val ottResponse = OttResponse(
          goodsNomenclature = GoodsNomenclatureResponse(
            "id",
            "1234567890",
            Some("some measure unit"),
            Instant.EPOCH,
            None,
            List("test")
          ),
          categoryAssessmentRelationships = Seq(CategoryAssessmentRelationship("assessmentId")),
          includedElements = Seq(
            CategoryAssessmentResponse("assessmentId", "themeId", Nil, "regulationId"),
            ThemeResponse("otherThemeId", 2, "theme description")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(
          ottResponse,
          "BV",
          "1234567890",
          testTraderProfileResponseWithoutNiphlAndNirms
        ) mustBe None
      }

      "when a certificate cannot be found" in {

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
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId1", 1, "theme description"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(
          ottResponse,
          "BV",
          "commodity code",
          testTraderProfileResponseWithoutNiphlAndNirms
        ) mustBe None
      }

      "when an additional code cannot be found" in {

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
              ),
              "regulationId1"
            ),
            ThemeResponse("themeId1", 1, "theme description"),
            CertificateResponse("exemptionId1", "code1", "description1")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(
          ottResponse,
          "BV",
          "commodity code",
          testTraderProfileResponseWithoutNiphlAndNirms
        ) mustBe None
      }

    }
  }

  "getAnswersForQuestions" - {

    "return answered and unanswered questions" - {

      "for an assessment" in {

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationInfo.getAnswersForQuestions(userAnswers, testRecordId) mustBe
          Seq(
            AnsweredQuestions(0, category1, Some(AssessmentAnswer.Exemption(Seq("Y903")))),
            AnsweredQuestions(1, category2, Some(AssessmentAnswer.NoExemption)),
            AnsweredQuestions(2, category3, None)
          )

      }

      "for a longer commodity code reassessment" in {

        val longerCommodity = categorisationInfo.copy(
          longerCode = true,
          categoryAssessments = Seq(category1, category1, category2, category3),
          categoryAssessmentsThatNeedAnswers = Seq(category1, category1, category2, category3)
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("Y903")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value
          .set(LongerCategorisationDetailsQuery(testRecordId), longerCommodity)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NoExemption))
          .success
          .value

        longerCommodity.getAnswersForQuestions(userAnswers, testRecordId) mustBe
          Seq(
            AnsweredQuestions(0, category1, Some(AssessmentAnswer.Exemption(Seq("Y903"))), reassessmentQuestion = true),
            AnsweredQuestions(1, category1, Some(AssessmentAnswer.Exemption(Seq("Y903"))), reassessmentQuestion = true),
            AnsweredQuestions(2, category2, Some(AssessmentAnswer.NoExemption), reassessmentQuestion = true),
            AnsweredQuestions(3, category3, None, reassessmentQuestion = true)
          )

      }

    }

  }

  "getAssessmentFromIndex" - {
    val assessments = Seq(
      CategoryAssessment(
        "assessmentId2",
        2,
        Seq(
          Certificate("exemptionId1", "code1", "description1"),
          AdditionalCode("exemptionId2", "code2", "description2")
        ),
        "theme description",
        Some("regulationUrl2")
      )
    )

    val categoryInfo = CategorisationInfo(
      commodityCode = "1234567890",
      countryOfOrigin = "BV",
      comcodeEffectiveToDate = Some(validityEndDate),
      categoryAssessments = assessments,
      categoryAssessmentsThatNeedAnswers = assessments,
      measurementUnit = Some("some measure unit"),
      descendantCount = 0
    )
    "return assessment when assessment index is in range" in {
      categoryInfo.getAssessmentFromIndex(0) mustBe Some(assessments.head)
    }

    "return none when assessment index out of range" in {
      categoryInfo.getAssessmentFromIndex(1) mustBe None
    }

  }

  "getMinimalCommodityCode" - {

    "must not remove non-trailing zeros" in {
      val categoryInfo = CategorisationInfo("1234500001", "BV", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "1234500001"
    }

    "must remove trailing zeros to make it 6 digits" in {
      val categoryInfo = CategorisationInfo("1234560000", "BV", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "123456"
    }

    "must not remove trailing zeros that would make it less than 6 digits" in {
      val categoryInfo = CategorisationInfo("1234000000", "BV", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "123400"
    }

  }

}
