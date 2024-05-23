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

import models.{AssessmentAnswer, UserAnswers}
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import queries.CategorisationQuery

class AssessmentPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".cleanup" - {

    val assessment1        = CategoryAssessment(
      "id1",
      1,
      Seq(Certificate("cert1", "code1", "description1"), Certificate("cert11", "code11", "description11"))
    )
    val assessment2        = CategoryAssessment(
      "id2",
      2,
      Seq(Certificate("cert2", "code2", "description2"), Certificate("cert22", "code22", "description222"))
    )
    val assessment3        = CategoryAssessment(
      "id3",
      3,
      Seq(Certificate("cert3", "code3", "description3"), Certificate("cert33", "code33", "description33"))
    )
    val categorisationInfo = CategorisationInfo("123", Seq(assessment1, assessment2, assessment3))

    "must not remove any assessments when an assessment is answered with an exemption" in {

      val answers =
        UserAnswers("id")
          .set(CategorisationQuery, categorisationInfo)
          .success
          .value
          .set(AssessmentPage(assessment1.id), AssessmentAnswer.Exemption("cert1"))
          .success
          .value
          .set(AssessmentPage(assessment2.id), AssessmentAnswer.Exemption("cert2"))
          .success
          .value
          .set(AssessmentPage(assessment3.id), AssessmentAnswer.Exemption("cert3"))
          .success
          .value

      val result = answers.set(AssessmentPage(assessment2.id), AssessmentAnswer.Exemption("cert22")).success.value

      result.isDefined(AssessmentPage(assessment1.id)) mustBe true
      result.isDefined(AssessmentPage(assessment2.id)) mustBe true
      result.isDefined(AssessmentPage(assessment3.id)) mustBe true
    }

    "must remove all assessments later in the list when an assessment is answered with no exemption" in {

      val answers =
        UserAnswers("id")
          .set(CategorisationQuery, categorisationInfo)
          .success
          .value
          .set(AssessmentPage(assessment1.id), AssessmentAnswer.Exemption("cert1"))
          .success
          .value
          .set(AssessmentPage(assessment2.id), AssessmentAnswer.Exemption("cert2"))
          .success
          .value
          .set(AssessmentPage(assessment3.id), AssessmentAnswer.Exemption("cert3"))
          .success
          .value

      val result = answers.set(AssessmentPage(assessment2.id), AssessmentAnswer.NoExemption).success.value

      result.isDefined(AssessmentPage(assessment1.id)) mustBe true
      result.isDefined(AssessmentPage(assessment2.id)) mustBe true
      result.isDefined(AssessmentPage(assessment3.id)) mustBe false
    }
  }
}
