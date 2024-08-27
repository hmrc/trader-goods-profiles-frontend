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
import base.TestConstants.{NiphlsCode, testRecordId}
import models.ott.response._
import models.{AnsweredQuestions, AssessmentAnswer, TraderProfile}
import pages.{AssessmentPage, ReassessmentPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}

import java.time.Instant

class CategorisationInfoSpec extends SpecBase {

  val testTraderProfileResponseWithoutNiphl: TraderProfile = TraderProfile("actorId", "ukims number", None, None)

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

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          Some("some measure unit"),
          0
        )

        val result = CategorisationInfo.build(ottResponse, "1234567890", testTraderProfileResponseWithoutNiphl)
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
              Seq(ExemptionResponse("exemptionId1", ExemptionType.Certificate))
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

        val expectedAssessments = Seq(
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
            Seq(Certificate("exemptionId1", "code1", "description1"))
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = expectedAssessments,
          categoryAssessmentsThatNeedAnswers = expectedAssessments,
          Some("some measure unit"),
          2
        )

        val result = CategorisationInfo.build(ottResponse, "1234567890", testTraderProfileResponseWithoutNiphl)
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

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "123456",
          comcodeEffectiveToDate = Some(validityEndDate),
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          None,
          0
        )

        val result = CategorisationInfo.build(ottResponse, "123456", testTraderProfileResponseWithoutNiphl)
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

        val expectedResult = CategorisationInfo("1234567890", None, Seq.empty, Seq.empty, None, 0)

        val result =
          CategorisationInfo.build(ottResponseNoAssessments, "1234567890", testTraderProfileResponseWithoutNiphl)
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
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId1",
              Seq.empty
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId1",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode))
            )
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessments = Seq(
          CategoryAssessment(
            "assessmentId1",
            1,
            Seq.empty
          ),
          CategoryAssessment(
            "assessmentId3",
            1,
            Seq(AdditionalCode("exemptionId2", "code2", "description2"))
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult =
          CategorisationInfo("1234567890", None, expectedAssessments, Seq.empty, None, 0)

        val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithoutNiphl)
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
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId2",
              Seq.empty
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId2",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode))
            )
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessments = Seq(
          CategoryAssessment(
            "assessmentId1",
            2,
            Seq.empty
          ),
          CategoryAssessment(
            "assessmentId3",
            2,
            Seq(AdditionalCode("exemptionId2", "code2", "description2"))
          ),
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult =
          CategorisationInfo("1234567890", None, expectedAssessments, Seq.empty, None, 0)

        val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithoutNiphl)
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
            ThemeResponse("themeId1", 1),
            CategoryAssessmentResponse(
              "assessmentId2",
              "themeId1",
              Seq(
                ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
              )
            ),
            ThemeResponse("themeId2", 2),
            CertificateResponse("exemptionId1", "code1", "description1"),
            AdditionalCodeResponse("exemptionId2", "code2", "description2"),
            ThemeResponse("ignoredTheme", 3),
            CertificateResponse("ignoredExemption", "code3", "description3"),
            CategoryAssessmentResponse(
              "assessmentId1",
              "themeId2",
              Seq.empty
            ),
            CategoryAssessmentResponse(
              "assessmentId3",
              "themeId1",
              Seq(ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode))
            )
          ),
          descendents = Seq.empty[Descendant]
        )

        val expectedAssessmentId1 = CategoryAssessment(
          "assessmentId1",
          2,
          Seq.empty
        )
        val expectedAssesmentId2  = CategoryAssessment(
          "assessmentId2",
          1,
          Seq(
            Certificate("exemptionId1", "code1", "description1"),
            AdditionalCode("exemptionId2", "code2", "description2")
          )
        )
        val expectedAssessmentId3 = CategoryAssessment(
          "assessmentId3",
          1,
          Seq(AdditionalCode("exemptionId2", "code2", "description2"))
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
            Some(validityEndDate),
            expectedAssessments,
            expectedAssessmentsThatNeedAnswers,
            None,
            0
          )

        val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithoutNiphl)
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

        val assessments = Seq(
          CategoryAssessment(
            "assessmentId2",
            2,
            Seq(
              Certificate("exemptionId1", "code1", "description1"),
              AdditionalCode("exemptionId2", "code2", "description2")
            )
          )
        )

        val expectedResult = CategorisationInfo(
          commodityCode = "1234567890",
          comcodeEffectiveToDate = None,
          categoryAssessments = assessments,
          categoryAssessmentsThatNeedAnswers = assessments,
          Some("some measure unit"),
          0,
          longerCode = true
        )

        val result =
          CategorisationInfo.build(ottResponse, "1234567890", testTraderProfileResponseWithoutNiphl, longerCode = true)
        result.value mustEqual expectedResult
      }

      "when NIPHL" - {

        ".is authorised" - {

          val testTraderProfileResponseWithNiphl =
            TraderProfile("actorId", "ukims number", Some("nirms number"), Some("niphl number"))

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
                ThemeResponse("themeId1", 1),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  )
                ),
                ThemeResponse("themeId2", 2),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("ignoredTheme", 3),
                CertificateResponse("ignoredExemption", "code3", "description3"),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId2",
                  Seq.empty
                )
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1 = CategoryAssessment(
              "assessmentId1",
              2,
              Seq.empty
            )
            val expectedAssesmentId2  = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              )
            )

            val expectedAssessments = Seq(
              expectedAssesmentId2,
              expectedAssessmentId1
            )

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssesmentId2)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNiphlsAuthorised = true
              )

            val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithNiphl)
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
                ThemeResponse("themeId1", 1),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  )
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 1),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NiphlsCode, ExemptionType.Certificate)
                  )
                ),
                CertificateResponse(NiphlsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty
                )
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              )
            )
            val expectedNiphlAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate(NiphlsCode, "WFE-code", "WFE-description")
              )
            )
            val expectedAssessmentId3     = CategoryAssessment(
              "assessmentId3",
              2,
              Seq.empty
            )

            val expectedAssessments = Seq(
              expectedNiphlAssesmentId2,
              expectedAssessmentId1,
              expectedAssessmentId3
            )

            val testTraderProfileResponseWithNiphl =
              TraderProfile("actorId", "ukims number", Some("nirms number"), Some("niphl number"))

            val expectedAssessmentsThatNeedAnswers = Seq(expectedAssessmentId1)

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0,
                isTraderNiphlsAuthorised = true
              )

            val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithNiphl)
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
                ThemeResponse("themeId1", 1),
                CategoryAssessmentResponse(
                  "assessmentId1",
                  "themeId1",
                  Seq(
                    ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                    ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                  )
                ),
                CertificateResponse("exemptionId1", "code1", "description1"),
                AdditionalCodeResponse("exemptionId2", "code2", "description2"),
                ThemeResponse("themeId2", 1),
                CategoryAssessmentResponse(
                  "assessmentId2",
                  "themeId2",
                  Seq(
                    ExemptionResponse(NiphlsCode, ExemptionType.Certificate)
                  )
                ),
                CertificateResponse(NiphlsCode, "WFE-code", "WFE-description"),
                ThemeResponse("themeId3", 2),
                CategoryAssessmentResponse(
                  "assessmentId3",
                  "themeId3",
                  Seq.empty
                )
              ),
              descendents = Seq.empty[Descendant]
            )

            val expectedAssessmentId1     = CategoryAssessment(
              "assessmentId1",
              1,
              Seq(
                Certificate("exemptionId1", "code1", "description1"),
                AdditionalCode("exemptionId2", "code2", "description2")
              )
            )
            val expectedNiphlAssesmentId2 = CategoryAssessment(
              "assessmentId2",
              1,
              Seq(
                Certificate(NiphlsCode, "WFE-code", "WFE-description")
              )
            )
            val expectedAssessmentId3     = CategoryAssessment(
              "assessmentId3",
              2,
              Seq.empty
            )

            val expectedAssessments = Seq(
              expectedNiphlAssesmentId2,
              expectedAssessmentId1,
              expectedAssessmentId3
            )

            val expectedAssessmentsThatNeedAnswers = Seq.empty

            val expectedResult =
              CategorisationInfo(
                "1234567890",
                expectedAssessments,
                expectedAssessmentsThatNeedAnswers,
                None,
                0
              )

            val result = CategorisationInfo.build(mockOttResponse, "1234567890", testTraderProfileResponseWithoutNiphl)

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
            ThemeResponse("otherThemeId", 2)
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(ottResponse, "1234567890", testTraderProfileResponseWithoutNiphl) mustBe None
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
            CategoryAssessmentResponse("assessmentId", "themeId", Nil),
            ThemeResponse("otherThemeId", 2)
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(ottResponse, "1234567890", testTraderProfileResponseWithoutNiphl) mustBe None
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
              )
            ),
            ThemeResponse("themeId1", 1),
            AdditionalCodeResponse("exemptionId2", "code2", "description2")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(ottResponse, "commodity code", testTraderProfileResponseWithoutNiphl) mustBe None
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
              )
            ),
            ThemeResponse("themeId1", 1),
            CertificateResponse("exemptionId1", "code1", "description1")
          ),
          descendents = Seq.empty[Descendant]
        )

        CategorisationInfo.build(ottResponse, "commodity code", testTraderProfileResponseWithoutNiphl) mustBe None
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
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value

        categorisationInfo.getAnswersForQuestions(userAnswers, testRecordId) mustBe
          Seq(
            AnsweredQuestions(0, category1, Some(AssessmentAnswer.Exemption)),
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
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
          .success
          .value
          .set(LongerCategorisationDetailsQuery(testRecordId), longerCommodity)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
          .success
          .value

        longerCommodity.getAnswersForQuestions(userAnswers, testRecordId) mustBe
          Seq(
            AnsweredQuestions(0, category1, Some(AssessmentAnswer.Exemption), reassessmentQuestion = true),
            AnsweredQuestions(1, category1, Some(AssessmentAnswer.Exemption), reassessmentQuestion = true),
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
        )
      )
    )

    val categoryInfo = CategorisationInfo(
      commodityCode = "1234567890",
      comcodeEffectiveToDate = Some(validityEndDate),
      categoryAssessments = assessments,
      categoryAssessmentsThatNeedAnswers = assessments,
      Some("some measure unit"),
      0
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
      val categoryInfo = CategorisationInfo("1234500001", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "1234500001"
    }

    "must remove trailing zeros to make it 6 digits" in {
      val categoryInfo = CategorisationInfo("1234560000", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "123456"
    }

    "must not remove trailing zeros that would make it less than 6 digits" in {
      val categoryInfo = CategorisationInfo("1234000000", Some(validityEndDate), Seq.empty, Seq.empty, None, 0)
      categoryInfo.getMinimalCommodityCode mustBe "123400"
    }

  }

}
