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
import utils.Clock.todayInstant

import java.time.Instant

class CommoditySpec extends SpecBase {

  extension (instant: Instant) {
    def daysToSeconds(days: Int): Int = days * 24 * 60 * 60
    def minusDays(days: Int): Instant = instant.minusSeconds(daysToSeconds(days))
    def plusDays(days: Int): Instant  = instant.plusSeconds(daysToSeconds(days))
  }

  "Commodity" - {
    "isValid" - {
      "must return true when" - {
        "validityEndDate is None and today is the same as validityStartDate" in {
          val commodity = Commodity("commodityCode", List("description"), Instant.now, None)
          commodity.isValid mustBe true
        }
        "validityEndDate is None and today is after validityStartDate" in {
          val commodity = Commodity("commodityCode", List("description"), Instant.now.minusDays(1), None)
          commodity.isValid mustBe true
        }
        "validityEndDate is today and validityStartDate is today" in {
          val exactTodayInstant: Instant = Instant.now
          val commodity                  = Commodity("commodityCode", List("description"), exactTodayInstant, Some(exactTodayInstant))
          commodity.isValid mustBe true
        }
        "validityEndDate is today and today is after validityStartDate" in {
          val commodity = Commodity("commodityCode", List("description"), Instant.now.minusDays(1), Some(todayInstant))
          commodity.isValid mustBe true
        }
        "validityEndDate is after today and today is the validityStartDate" in {
          val commodity = Commodity("commodityCode", List("description"), Instant.now, Some(Instant.now.plusDays(1)))
          commodity.isValid mustBe true
        }
        "validityEndDate is after today and today is after validityStartDate" in {
          val commodity =
            Commodity("commodityCode", List("description"), Instant.now.minusDays(1), Some(Instant.now.plusDays(1)))
          commodity.isValid mustBe true
        }

      }

      "must return false when" - {
        "validityEndDate has past" in {
          val commodity =
            Commodity("commodityCode", List("description"), Instant.now.minusDays(1), Some(Instant.now.minusDays(1)))
          commodity.isValid mustBe false
        }

        "validityStartDate has not been reached" in {
          val commodity = Commodity("commodityCode", List("description"), Instant.now.plusDays(1), None)
          commodity.isValid mustBe false
        }
      }
    }
  }
}
