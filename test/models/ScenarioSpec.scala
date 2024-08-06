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
import base.TestConstants.{testEori, testRecordId}
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import utils.Constants.{Category1AsInt, Category2AsInt, StandardGoodsAsInt}

class ScenarioSpec extends SpecBase {

  ".getResult" - {

    "must return 3 for standard goods scenario" in {
      Scenario2.getResultAsInt(StandardGoodsScenario) mustBe StandardGoodsAsInt
    }

    "must return 2 for category 2 scenario" in {
      Scenario2.getResultAsInt(Category2Scenario) mustBe Category2AsInt
    }

    "must return 1 for category 1 scenario" in {
      Scenario2.getResultAsInt(Category1Scenario) mustBe Category1AsInt
    }

  }

  ".getRedirectScenarios" - {

    "must return correct RedirectScenario" - {

      "Category1NoExemptions" - {

        "when category 1 assessments and they have no exemptions" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("id1", 1, Seq.empty),
              CategoryAssessment("id2", 2, Seq.empty)
            ),
            None,
            0
          )

          Scenario.getRedirectScenarios(categorisationInfo) mustEqual Category1NoExemptions
        }

        "when some category 1 assessments have exemptions and others do not" in {
          val categorisationInfo = CategorisationInfo(
            "1234567890",
            Seq(
              CategoryAssessment("id1", 1, Seq(Certificate("cert", "code", "desc"))),
              CategoryAssessment("id2", 1, Seq.empty)
            ),
            None,
            0
          )

          Scenario.getRedirectScenarios(categorisationInfo) mustEqual Category1NoExemptions
        }
      }

      "StandardNoAssessments when no assessments" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq.empty,
          None,
          0
        )

        Scenario.getRedirectScenarios(categorisationInfo) mustEqual StandardNoAssessments

      }

    }

    "must return NoRedirectScenario" - {

      "NoRedirectScenario when there are Category 1 assessments with exemptions" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            CategoryAssessment("id1", 1, Seq(Certificate("123", "cert1", "certificate"))),
            CategoryAssessment("id2", 2, Seq.empty)
          ),
          None,
          0
        )

        Scenario.getRedirectScenarios(categorisationInfo) mustEqual NoRedirectScenario
      }

      "NoRedirectScenario when there are no Category 1 assessments but are Category 2" in {
        val categorisationInfo = CategorisationInfo(
          "1234567890",
          Seq(
            CategoryAssessment("id1", 2, Seq(Certificate("123", "cert1", "certificate")))
          ),
          None,
          0
        )

        Scenario.getRedirectScenarios(categorisationInfo) mustEqual NoRedirectScenario
      }
    }

  }

  ".getScenario" - {

    "must return correct Scenario" - {

      "Category1 when category is 1" in {

        val goodsRecord = CategoryRecord(
          eori = testEori,
          recordId = testRecordId,
          category = 1,
          categoryAssessmentsWithExemptions = 3,
          measurementUnit = Some("1")
        )

        val result = Scenario.getScenario(goodsRecord)

        result mustEqual Category1
      }

      "Category2 when category is 2" in {

        val goodsRecord = CategoryRecord(
          eori = testEori,
          recordId = testRecordId,
          category = 2,
          categoryAssessmentsWithExemptions = 3,
          measurementUnit = Some("1")
        )

        val result = Scenario.getScenario(goodsRecord)

        result mustEqual Category2
      }

      "Standard when category is 3" in {

        val goodsRecord = CategoryRecord(
          eori = testEori,
          recordId = testRecordId,
          category = 3,
          categoryAssessmentsWithExemptions = 3,
          measurementUnit = Some("1")
        )

        val result = Scenario.getScenario(goodsRecord)

        result mustEqual Standard
      }
    }
  }
}
