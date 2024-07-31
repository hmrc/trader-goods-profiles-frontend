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
import queries.CategorisationDetailsQuery

class CategoryRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    val assessment1 = CategoryAssessment("assessmentId1", 1, Seq(Certificate("1", "code", "description")))
    val assessment2 = CategoryAssessment("assessmentId2", 1, Seq(Certificate("1", "code", "description")))
    val assessment3 = CategoryAssessment("assessmentId3", 2, Seq(Certificate("1", "code", "description")))
    val assessment4 = CategoryAssessment("assessmentId4", 2, Seq(Certificate("1", "code", "description")))

    val categorisationInfo               =
      CategorisationInfo("123", Seq(assessment1, assessment2, assessment3, assessment4), Some("kg"), 0)
    val noCategory1CategorisationInfo    = CategorisationInfo("123", Seq(assessment3), Some("kg"), 0)
    val noCategory1Or2CategorisationInfo = CategorisationInfo("123", Seq(), Some("kg"), 0)
    val noCategory2CategorisationInfo    = CategorisationInfo("123", Seq(assessment1), Some("kg"), 0)

    "must return a CategoryRecord when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1.0")
            .success
            .value
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            None,
            1,
            0,
            Some("1.0"),
            Some("kg")
          )
        )
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            None,
            1,
            0,
            None,
            Some("kg")
          )
        )
      }

      "where 1st question is No Exemption so its category 1" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            None,
            1,
            0,
            None,
            Some("kg")
          )
        )
      }

      "where last cat 1 question is No Exemption so its category 1" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            None,
            1,
            1,
            None,
            Some("kg")
          )
        )
      }

      "where first cat 2 question is No Exemption so its category 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            None,
            2,
            2,
            None,
            Some("kg")
          )
        )
      }

      "where last cat 2 question is No Exemption so its category 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            None,
            2,
            3,
            None,
            Some("kg")
          )
        )
      }

      "where last cat 2 question is completed so its category 3" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            None,
            3,
            4,
            None,
            Some("kg")
          )
        )
      }

      "where recordCategorisations is missing category 1 assessments but completed category 2 so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(CategorisationDetailsQuery(testRecordId), noCategory1CategorisationInfo)
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
            None,
            3,
            1,
            None,
            Some("kg")
          )
        )
      }

      "when recordCategorisations is missing category 2 assessments but completed category 1 so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(CategorisationDetailsQuery(testRecordId), noCategory2CategorisationInfo)
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
            None,
            3,
            1,
            None,
            Some("kg")
          )
        )
      }

      "when recordCategorisations is missing any assessments so is category 3" in {

        val answers = UserAnswers(userAnswersId)
          .set(CategorisationDetailsQuery(testRecordId), noCategory1Or2CategorisationInfo)
          .success
          .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            None,
            3,
            0,
            None,
            Some("kg")
          )
        )
      }

    }

    "must return errors" - {

      "when all mandatory answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(CategorisationDetailsQuery(testRecordId)),
            PageMissing(CategorisationDetailsQuery(testRecordId))
          ) // TODO Same error twice???
        }
      }

      "when the user said they have a SupplementaryUnit but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
            .set(SupplementaryUnitPage(testRecordId), "1.0")
            .success
            .value
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
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
