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
import utils.Constants.{Category1AsInt, Category2AsInt, StandardGoodsAsInt}

class ScenarioSpec extends SpecBase {

  ".getResult" - {

    "must return 3 for standard goods scenario" in {
      Scenario.getResultAsInt(StandardGoodsScenario) mustBe StandardGoodsAsInt
    }

    "must return 3 for standard goods no assessment scenario" in {
      Scenario.getResultAsInt(StandardGoodsNoAssessmentsScenario) mustBe StandardGoodsAsInt
    }

    "must return 2 for category 2 scenario" in {
      Scenario.getResultAsInt(Category2Scenario) mustBe Category2AsInt
    }

    "must return 1 for category 1 scenario" in {
      Scenario.getResultAsInt(Category1Scenario) mustBe Category1AsInt
    }

    "must return 1 for category 1 no exemptions scenario" in {
      Scenario.getResultAsInt(Category1NoExemptionsScenario) mustBe Category1AsInt
    }
    ".fromInt" - {

      "must return Some(StandardGoodsScenario) for StandardGoodsAsInt" in {
        Scenario.fromInt(Some(StandardGoodsAsInt)) mustBe Some(StandardGoodsScenario)
      }

      "must return Some(Category1Scenario) for Category1AsInt" in {
        Scenario.fromInt(Some(Category1AsInt)) mustBe Some(Category1Scenario)
      }

      "must return Some(Category2Scenario) for Category2AsInt" in {
        Scenario.fromInt(Some(Category2AsInt)) mustBe Some(Category2Scenario)
      }

      "must return None for unknown integer" in {
        Scenario.fromInt(Some(999)) mustBe None
      }

      "must return None for None input" in {
        Scenario.fromInt(None) mustBe None
      }
    }

  }
}
