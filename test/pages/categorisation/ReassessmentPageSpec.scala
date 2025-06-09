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

package pages.categorisation

import base.SpecBase
import base.TestConstants.testRecordId
import models.ott._
import models.{AssessmentAnswer, ReassessmentAnswer}
import queries.LongerCategorisationDetailsQuery

class ReassessmentPageSpec extends SpecBase {

  ".cleanup" - {

    val assessment1        = CategoryAssessment(
      "id1",
      1,
      Seq(Certificate("cert1", "code1", "description1"), Certificate("cert11", "code11", "description11")),
      "measure description",
      Some("regulationUrl1")
    )
    val assessment2        = CategoryAssessment(
      "id2",
      2,
      Seq(Certificate("cert2", "code2", "description2"), Certificate("cert22", "code22", "description222")),
      "measure description",
      Some("regulationUrl2")
    )
    val assessment3        = CategoryAssessment(
      "id3",
      3,
      Seq(Certificate("cert3", "code3", "description3"), Certificate("cert33", "code33", "description33")),
      "measure description",
      Some("regulationUrl3")
    )
    val assessment4        =
      CategoryAssessment(
        "id4",
        4,
        Seq(AdditionalCode("cert4", "code4", "description4")),
        "measure description",
        Some("regulationUrl4")
      )
    val assessmentList     = Seq(assessment1, assessment2, assessment3, assessment4)
    val categorisationInfo =
      CategorisationInfo("1234567890", "BV", Some(validityEndDate), assessmentList, assessmentList, None, 1)

    "must not remove any assessments" - {
      "when an assessment is answered with an exemption" in {

        val answers = emptyUserAnswers
          .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value

        val result = answers
          .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value

        result.isDefined(ReassessmentPage(testRecordId, 0)) mustBe true
        result.isDefined(ReassessmentPage(testRecordId, 1)) mustBe true
        result.isDefined(ReassessmentPage(testRecordId, 2)) mustBe true
      }

    }

    "must remove all assessments later in the list" - {
      "when an assessment is answered with no exemption" in {
        val answers = emptyUserAnswers
          .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value
          .set(ReassessmentPage(testRecordId, 3), ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE"))))
          .success
          .value

        val result =
          answers.set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NoExemption)).success.value

        result.isDefined(ReassessmentPage(testRecordId, 0)) mustBe true
        result.isDefined(ReassessmentPage(testRecordId, 1)) mustBe true
        result.isDefined(ReassessmentPage(testRecordId, 2)) mustBe false
        result.isDefined(ReassessmentPage(testRecordId, 3)) mustBe false
      }
    }
  }
}
