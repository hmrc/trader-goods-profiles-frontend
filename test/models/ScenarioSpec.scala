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
import play.api.i18n.Messages
import utils.Constants.{Category1AsInt, Category2AsInt, StandardGoodsAsInt}
import play.api.mvc.JavascriptLiteral

class ScenarioSpec extends SpecBase {

  given testMessages: Messages with
    def apply(key: String, args: Any*): String = key

    def apply(keys: Seq[String], args: Seq[Any]): String =
      keys.headOption.getOrElse("")

    def asJava: play.i18n.Messages =
      throw new UnsupportedOperationException("asJava not supported")

    def lang: play.api.i18n.Lang =
      throw new UnsupportedOperationException("lang not supported")

    def translate(key: String, args: Seq[Any]): Option[String] = None

    def isDefinedAt(key: String): Boolean = true

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

  "Scenario messageKey and toString" - {

    "StandardGoodsScenario" in {
      StandardGoodsScenario.messageKey mustBe "singleRecord.standardGoods"
      StandardGoodsScenario.toString(testMessages) mustBe "singleRecord.standardGoods"
    }

    "Category1Scenario" in {
      Category1Scenario.messageKey mustBe "singleRecord.cat1"
      Category1Scenario.toString(testMessages) mustBe "singleRecord.cat1"
    }

    "Category2Scenario" in {
      Category2Scenario.messageKey mustBe "singleRecord.cat2"
      Category2Scenario.toString(testMessages) mustBe "singleRecord.cat2"
    }

    "StandardGoodsNoAssessmentsScenario" in {
      StandardGoodsNoAssessmentsScenario.messageKey mustBe "singleRecord.standardGoods"
      StandardGoodsNoAssessmentsScenario.toString(testMessages) mustBe "singleRecord.standardGoods"
    }

    "Category1NoExemptionsScenario" in {
      Category1NoExemptionsScenario.messageKey mustBe "singleRecord.cat1"
      Category1NoExemptionsScenario.toString(testMessages) mustBe "singleRecord.cat1"
    }

    "Category2NoExemptionsScenario" in {
      Category2NoExemptionsScenario.messageKey mustBe "singleRecord.cat2"
      Category2NoExemptionsScenario.toString(testMessages) mustBe "singleRecord.cat2"
    }
  }

  ".jsLiteral" - {
    import Scenario.jsLiteral

    "must convert StandardGoodsScenario to 'Standard'" in {
      implicitly[JavascriptLiteral[Scenario]].to(StandardGoodsScenario) mustBe "Standard"
    }

    "must convert Category1Scenario to 'Category1'" in {
      implicitly[JavascriptLiteral[Scenario]].to(Category1Scenario) mustBe "Category1"
    }

    "must convert Category2Scenario to 'Category2'" in {
      implicitly[JavascriptLiteral[Scenario]].to(Category2Scenario) mustBe "Category2"
    }

    "must convert StandardGoodsNoAssessmentsScenario to 'StandardNoAssessments'" in {
      implicitly[JavascriptLiteral[Scenario]].to(StandardGoodsNoAssessmentsScenario) mustBe "StandardNoAssessments"
    }

    "must convert Category1NoExemptionsScenario to 'Category1NoExemptions'" in {
      implicitly[JavascriptLiteral[Scenario]].to(Category1NoExemptionsScenario) mustBe "Category1NoExemptions"
    }

    "must convert Category2NoExemptionsScenario to 'Category2NoExemptions'" in {
      implicitly[JavascriptLiteral[Scenario]].to(Category2NoExemptionsScenario) mustBe "Category2NoExemptions"
    }
  }
}
