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
import queries.CategorisationQuery

class CategoryRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    val assessment1 = CategoryAssessment("assessmentId1", 1, Seq(Certificate("1", "code", "description")))
    val assessment2 = CategoryAssessment("assessmentId2", 1, Seq(Certificate("1", "code", "description")))
    val assessment3 = CategoryAssessment("assessmentId3", 2, Seq(Certificate("1", "code", "description")))
    val assessment4 = CategoryAssessment("assessmentId4", 2, Seq(Certificate("1", "code", "description")))

    val categorisationInfo = CategorisationInfo("123", Seq(assessment1, assessment2, assessment3, assessment4))

    "must return a CategoryRecord when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            Some(1),
            Some("1")
          )
        )
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, false)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
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

      "where last question answered is in cat 1 and cat 1 not complete so category is 1" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value
            .set(AssessmentPage("assessmentId1"), AssessmentAnswer.NoExemption)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            1,
            Some(1),
            Some("1")
          )
        )
      }

      "where last question answered is in cat 1 and cat 1 complete so category is 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value
            .set(AssessmentPage("assessmentId2"), AssessmentAnswer.Exemption("cert2"))
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            2,
            Some(1),
            Some("1")
          )
        )
      }

      "where last question answered is in cat 2 and cat 2 not complete so category is 2" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value
            .set(AssessmentPage("assessmentId2"), AssessmentAnswer.Exemption("cert2"))
            .success
            .value
            .set(AssessmentPage("assessmentId3"), AssessmentAnswer.Exemption("cert3"))
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            2,
            Some(1),
            Some("1")
          )
        )
      }

      "where last question answered is in cat 2 and cat 2 complete so category is 3" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value
            .set(AssessmentPage("assessmentId2"), AssessmentAnswer.Exemption("cert2"))
            .success
            .value
            .set(AssessmentPage("assessmentId4"), AssessmentAnswer.Exemption("cert4"))
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        result mustEqual Right(
          CategoryRecord(
            testEori,
            testRecordId,
            3,
            Some(1),
            Some("1")
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
            PageMissing(HasSupplementaryUnitPage),
            PageMissing(CategorisationQuery)
          )
        }
      }

      "when the user said they have a SupplementaryUnit but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage)
        }
      }

      "when the user said they don't have optional data but it is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, false)
            .success
            .value
            .set(SupplementaryUnitPage, 1)
            .success
            .value
            .set(CategorisationQuery, categorisationInfo)
            .success
            .value

        val result = CategoryRecord.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage)
        }
      }
    }
  }
}
