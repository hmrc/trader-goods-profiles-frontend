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
import base.TestConstants.{testRecordId, userAnswersId}
import models.{Commodity, UserAnswers}
import pages.{HasCorrectGoodsLongerCommodityCodePage, LongerCommodityCodePage}
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}

import java.time.Instant

class LongerCommodityCodePageSpec extends SpecBase {

  "clean up" - {

    "does not removes HasCorrectGoodsLongerCommodityCodePage when longer commodity is same" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
        .success
        .value

      val result = userAnswers
        .set(
          LongerCommodityQuery(testRecordId),
          Commodity("1234567890", List("Description", "Other"), Instant.now, None)
        )
        .success
        .value
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
        .success
        .value
        .set(LongerCommodityCodePage(testRecordId), "7890")
        .success
        .value

      result.isDefined(HasCorrectGoodsLongerCommodityCodePage(testRecordId)) mustBe true

      result.get(HasCorrectGoodsLongerCommodityCodePage(testRecordId)) mustBe Some(true)
    }

    "removes HasCorrectGoodsLongerCommodityCodePage when longer commodity is different" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
        .success
        .value

      val result = userAnswers
        .set(
          LongerCommodityQuery(testRecordId),
          Commodity("1234567890", List("Description", "Other"), Instant.now, None)
        )
        .success
        .value
        .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
        .success
        .value
        .set(LongerCommodityCodePage(testRecordId), "1234")
        .success
        .value

      result.isDefined(HasCorrectGoodsLongerCommodityCodePage(testRecordId)) mustBe false

    }

  }
}
