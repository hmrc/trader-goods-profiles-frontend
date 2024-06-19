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

import base.TestConstants.{testEori, testRecordId}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class ScenarioSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".getScenario" - {

    "must return correct Scenario" - {

      "Category1 when category is 1" in {

        val goodsRecord = CategoryRecord(
          eori = testEori,
          recordId = testRecordId,
          category = 1,
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
          measurementUnit = Some("1")
        )

        val result = Scenario.getScenario(goodsRecord)

        result mustEqual Standard
      }
    }
  }
}
