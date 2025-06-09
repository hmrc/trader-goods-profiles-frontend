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

package models

import base.SpecBase
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Inside.inside
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.categorisation.{AssessmentPage, HasSupplementaryUnitPage, ReassessmentPage, SupplementaryUnitPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService

class CategoryRecordSpec extends SpecBase with BeforeAndAfterEach {
  private val mockCategorisationService = mock[CategorisationService]

  override def beforeEach(): Unit = {
    when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)
    super.beforeEach()
  }

  ".build" - {
    val assessment1 = CategoryAssessment(
      "assessmentId1",
      1,
      Seq(Certificate("1", "code", "description")),
      "measureTypeId1",
      Some("regulationId1")
    )
    val assessment2 = CategoryAssessment(
      "assessmentId2",
      1,
      Seq(Certificate("1", "code", "description")),
      "measureTypeId2",
      Some("regulationId2")
    )
    val assessment3 = CategoryAssessment(
      "assessmentId3",
      2,
      Seq(Certificate("1", "code", "description")),
      "measureTypeId3",
      Some("regulationId3")
    )
    val assessment4 = CategoryAssessment(
      "assessmentId4",
      2,
      Seq(Certificate("1", "code", "description")),
      "measureTypeId4",
      Some("regulationId3")
    )

    val assessmentList                = Seq(assessment1, assessment2, assessment3, assessment4)
    val categorisationInfo            =
      CategorisationInfo("1234567890", "BV", Some(validityEndDate), assessmentList, assessmentList, None, 1)
    val categorisationInfoMeasureUnit =
      CategorisationInfo("1234567890", "BV", Some(validityEndDate), assessmentList, assessmentList, Some("Weight"), 1)

    "must return a CategoryRecord" - {

      "when all assessments are answered and no measurement unit" in {

        val answers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "1234567890",
            Category1Scenario,
            None,
            None,
            categorisationInfo,
            4,
            wasSupplementaryUnitAsked = false
          )
        )

        withClue("must have used the categorisation service to find the category") {
          verify(mockCategorisationService)
            .calculateResult(eqTo(categorisationInfo), eqTo(answers), eqTo(testRecordId))
        }
      }

      "when all assessments are answered and measurement unit is set but not answered" in {

        val answers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfoMeasureUnit)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
          .success
          .value
          .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
          .success
          .value
        val result  = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "1234567890",
            Category1Scenario,
            Some("Weight"),
            None,
            categorisationInfoMeasureUnit,
            4,
            wasSupplementaryUnitAsked = false
          )
        )

      }

      "when has supplementary unit is no" in {

        val answers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfoMeasureUnit)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(HasSupplementaryUnitPage(testRecordId), false)
            .success
            .value
        val result  = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "1234567890",
            Category1Scenario,
            Some("Weight"),
            None,
            categorisationInfoMeasureUnit,
            4,
            wasSupplementaryUnitAsked = true
          )
        )

      }

      "when has supplementary unit is yes and unit is set" in {

        val answers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfoMeasureUnit)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1234")
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "1234567890",
            Category1Scenario,
            Some("Weight"),
            Some("1234"),
            categorisationInfoMeasureUnit,
            4,
            wasSupplementaryUnitAsked = true
          )
        )

      }

      "where 1st question is No Exemption so the count of answered questions is different to the total count" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "1234567890",
            Category1Scenario,
            None,
            None,
            categorisationInfo,
            1,
            wasSupplementaryUnitAsked = false
          )
        )

        withClue("must have used the categorisation service to find the category") {
          verify(mockCategorisationService)
            .calculateResult(eqTo(categorisationInfo), eqTo(answers), eqTo(testRecordId))
        }
      }

      "when longer commodity reassessment questions have been answered" in {

        val shorterCat = categorisationInfo.copy(commodityCode = "123456")
        val longerCat  =
          categorisationInfo.copy(commodityCode = "9999999999", longerCode = true, measurementUnit = None)
        val answers    =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), shorterCat)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
            .success
            .value
            .set(LongerCategorisationDetailsQuery(testRecordId), longerCat)
            .success
            .value
            .set(
              ReassessmentPage(testRecordId, 0),
              ReassessmentAnswer(
                AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                isAnswerCopiedFromPreviousAssessment = true
              )
            )
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NoExemption))
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            "9999999999",
            Category1Scenario,
            None,
            None,
            shorterCat,
            4,
            wasSupplementaryUnitAsked = false,
            Some(longerCat),
            Some(2),
            Some(1)
          )
        )

        withClue("must have used the categorisation service to find the category") {
          verify(mockCategorisationService).calculateResult(eqTo(longerCat), eqTo(answers), eqTo(testRecordId))
        }
      }

    }

    "must return errors" - {

      "when record categorisation details are missing" in {

        val answers = emptyUserAnswers

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            NoCategorisationDetailsForRecordId(CategorisationDetailsQuery(testRecordId), testRecordId)
          )
        }
      }

      "when the user said they have a SupplementaryUnit but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage(testRecordId))
        }
      }

      "when the user said they don't have supplementary unit but it is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(HasSupplementaryUnitPage(testRecordId), false)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1.0")
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId, mockCategorisationService)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage(testRecordId))
        }
      }

    }

  }
}
