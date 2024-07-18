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

package pages

import models.{AssessmentAnswer, RecordCategorisations, UserAnswers}
import models.ott.{AdditionalCode, CategorisationInfo, CategoryAssessment, Certificate}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import queries.RecordCategorisationsQuery

class AssessmentPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".cleanup" - {

    val assessment1           = CategoryAssessment(
      "id1",
      1,
      Seq(Certificate("cert1", "code1", "description1"), Certificate("cert11", "code11", "description11"))
    )
    val assessment2           = CategoryAssessment(
      "id2",
      2,
      Seq(Certificate("cert2", "code2", "description2"), Certificate("cert22", "code22", "description222"))
    )
    val assessment3           = CategoryAssessment(
      "id3",
      3,
      Seq(Certificate("cert3", "code3", "description3"), Certificate("cert33", "code33", "description33"))
    )
    val assessment4           = CategoryAssessment(
      "id4",
      4,
      Seq(AdditionalCode("cert4", "code4", "description4"))
    )
    val categorisationInfo    =
      CategorisationInfo("123", Seq(assessment1, assessment2, assessment3, assessment4), Some("some measure unit"), 0)
    val recordId              = "321"
    val index                 = 0
    val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

    "must not remove any assessments" - {
      "when an assessment is answered with an exemption" in {

        val answers =
          UserAnswers("id")
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 2), AssessmentAnswer.Exemption("true"))
            .success
            .value

        val result = answers.set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true")).success.value

        result.isDefined(AssessmentPage(recordId, index)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 1)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 2)) mustBe true
      }

      "when an assessment is answered with no exemption and the shouldRedirectToCya flag is set" in {

        val answers =
          UserAnswers("id")
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 2), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 3), AssessmentAnswer.Exemption("true"))
            .success
            .value

        val result = answers
          .set(AssessmentPage(recordId, index + 1, shouldRedirectToCya = true), AssessmentAnswer.NoExemption)
          .success
          .value

        result.isDefined(AssessmentPage(recordId, index)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 1)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 2)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 3)) mustBe true
      }

    }

    "must remove all assessments later in the list" - {
      "when an assessment is answered with no exemption" in {

        val answers =
          UserAnswers("id")
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 2), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 3), AssessmentAnswer.Exemption("true"))
            .success
            .value

        val result = answers.set(AssessmentPage(recordId, index + 1), AssessmentAnswer.NoExemption).success.value

        result.isDefined(AssessmentPage(recordId, index)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 1)) mustBe true
        result.isDefined(AssessmentPage(recordId, index + 2)) mustBe false
        result.isDefined(AssessmentPage(recordId, index + 3)) mustBe false
      }

      "when the cleanup all flag is passed in" in {

        val answers =
          UserAnswers("id")
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 2), AssessmentAnswer.Exemption("true"))
            .success
            .value
            .set(AssessmentPage(recordId, index + 3), AssessmentAnswer.Exemption("true"))
            .success
            .value

        val result = answers.remove(AssessmentPage(recordId, index, cleanupAll = true)).success.value

        result.isDefined(AssessmentPage(recordId, index)) mustBe false
        result.isDefined(AssessmentPage(recordId, index + 1)) mustBe false
        result.isDefined(AssessmentPage(recordId, index + 2)) mustBe false
        result.isDefined(AssessmentPage(recordId, index + 3)) mustBe false
      }

    }
  }
}
