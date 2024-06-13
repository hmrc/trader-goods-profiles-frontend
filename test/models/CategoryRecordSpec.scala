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

import base.TestConstants.{testEori, testRecordId, userAnswersId}
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import queries.RecordCategorisationsQuery

class CategoryRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    val assessment1 = CategoryAssessment("assessmentId1", 1, Seq(Certificate("1", "code", "description")))
    val assessment2 = CategoryAssessment("assessmentId2", 1, Seq(Certificate("1", "code", "description")))
    val assessment3 = CategoryAssessment("assessmentId3", 2, Seq(Certificate("1", "code", "description")))
    val assessment4 = CategoryAssessment("assessmentId4", 2, Seq(Certificate("1", "code", "description")))

    val categorisationInfo                  =
      CategorisationInfo("123", Seq(assessment1, assessment2, assessment3, assessment4), Some("some measure unit"))
    val recordCategorisations               = RecordCategorisations(records = Map(testRecordId -> categorisationInfo))
    val emptyRecordCategorisations          = RecordCategorisations(records = Map())
    val noCategory1RecordCategorisations    =
      RecordCategorisations(records =
        Map(testRecordId -> CategorisationInfo("123", Seq(assessment3), Some("some measure unit")))
      )
    val noCategory1Or2RecordCategorisations =
      RecordCategorisations(records = Map(testRecordId -> CategorisationInfo("123", Seq(), Some("some measure unit"))))
    val noCategory2RecordCategorisations    =
      RecordCategorisations(records =
        Map(testRecordId -> CategorisationInfo("123", Seq(assessment1), Some("some measure unit")))
      )

    "must return a CategoryRecord when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), 1.0)
            .success
            .value
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            Some(1.0),
            Some("1")
          )
        )
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            None,
            Some("1")
          )
        )
      }

      "where 1st question is No Exemption so its category 1" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            None,
            Some("1")
          )
        )
      }

      "where last cat 1 question is No Exemption so its category 1" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            None,
            Some("1")
          )
        )
      }

      "where first cat 2 question is No Exemption so its category 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("cert2"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            2,
            None,
            Some("1")
          )
        )
      }

      "where last cat 2 question is No Exemption so its category 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("cert2"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("cert3"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            2,
            None,
            Some("1")
          )
        )
      }

      "where last cat 2 question is completed so its category 3" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("cert2"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("cert3"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.Exemption("cert4"))
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            3,
            None,
            Some("1")
          )
        )
      }

      "where recordCategorisations is missing category 1 assessments but completed category 2 so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, noCategory1RecordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
          .success
          .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            3,
            None,
            Some("1")
          )
        )
      }

      "when recordCategorisations is missing category 2 assessments but completed category 1 so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, noCategory2RecordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("cert1"))
          .success
          .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            3,
            None,
            Some("1")
          )
        )
      }

      "when recordCategorisations is missing any assessments so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, noCategory1Or2RecordCategorisations)
          .success
          .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            3,
            None,
            Some("1")
          )
        )
      }

    }

    "must return errors" - {

      "when recordCategorisations is missing recordId" in {

        val answers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, emptyRecordCategorisations)
          .success
          .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            RecordIdMissing(RecordCategorisationsQuery)
          )
        }
      }

      "when all mandatory answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(RecordCategorisationsQuery)
          )
        }
      }

      "when the user said they have a SupplementaryUnit but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage(testRecordId))
        }
      }

      "when the user said they don't have optional data but it is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage(testRecordId), false)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), 1.0)
            .success
            .value
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage(testRecordId))
        }
      }
    }
  }
}
