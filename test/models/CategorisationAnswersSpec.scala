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
import base.TestConstants.testRecordId
import models.AssessmentAnswer.{Exemption, NoExemption, NotAnsweredYet}
import models.ott.CategorisationInfo
import org.scalatest.Inside.inside
import pages.{AssessmentPage, HasSupplementaryUnitPage, SupplementaryUnitPage}
import queries.RecordCategorisationsQuery

class CategorisationAnswersSpec extends SpecBase {

  ".build" - {

    "must return a CategorisationAnswer when" - {

      "a NoExemption means some assessment pages are unanswered" in {
        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), NoExemption)
          .success
          .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), NoExemption), None)
        )
      }

      "an assessment answer has not been answered yet" in {

        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), NotAnsweredYet)
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), NoExemption)
          .success
          .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), NoExemption), None)
        )

      }
      "all assessments are answered and supplementary unit was not asked" in {

        val answers =
          userAnswersForCategorisation

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), Exemption("NC123"), Exemption("X812")), None)
        )
      }

      "and supplementary unit was asked for and the answer was no" in {

        val answers =
          userAnswersForCategorisation
            .set(HasSupplementaryUnitPage(testRecordId), false)
            .success
            .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), Exemption("NC123"), Exemption("X812")), None)
        )
      }

      "and supplementary unit was asked for and the answer was yes and it was supplied" in {

        val answers =
          userAnswersForCategorisation
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "42.0")
            .success
            .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), Exemption("NC123"), Exemption("X812")), Some("42.0"))
        )
      }

      "all category 1 are answered and category 2 have no exemptions" in {

        val categoryQuery = CategorisationInfo(
          "1234567890",
          Seq(category1, category2, category3.copy(exemptions = Seq.empty)),
          Some("Weight, in kilograms"),
          0
        )

        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categoryQuery)))
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("NC123"))
          .success
          .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        result mustEqual Right(
          CategorisationAnswers(Seq(Exemption("Y994"), Exemption("NC123")), None)
        )
      }

    }

    "must return errors" - {

      "when the user said they have a supplementary unit but it is missing" in {

        val answers =
          userAnswersForCategorisation
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage(testRecordId))
        }
      }

      "when the user has a supplementary unit without being asked about it " in {

        val answers =
          userAnswersForCategorisation
            .set(SupplementaryUnitPage(testRecordId), "42.0")
            .success
            .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage(testRecordId))
        }
      }

      "when additional assessments have been answered after a NoExemption" in {

        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), NoExemption)
          .success
          .value
          .set(AssessmentPage(testRecordId, 2), Exemption("X812"))
          .success
          .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedNoExemption(AssessmentPage(testRecordId, 1))
        }
      }

      "when you have not finished answering assessments" in {

        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), Exemption("NC123"))
          .success
          .value

        val result = CategorisationAnswers.build(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only MissingAssessmentAnswers(RecordCategorisationsQuery)
        }
      }

      "when no answers for the record Id" in {

        val answers = emptyUserAnswers
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value
          .set(AssessmentPage(testRecordId, 0), Exemption("Y994"))
          .success
          .value
          .set(AssessmentPage(testRecordId, 1), Exemption("NC123"))
          .success
          .value

        val result = CategorisationAnswers.build(answers, "differentId")

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only NoCategorisationDetailsForRecordId(
            RecordCategorisationsQuery,
            "differentId"
          )
        }
      }

    }
  }
}
