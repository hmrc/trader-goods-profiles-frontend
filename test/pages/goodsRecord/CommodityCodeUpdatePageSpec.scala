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

package pages.goodsRecord

import base.TestConstants.{testRecordId, userAnswersId}
import models.UserAnswers
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.HasCorrectGoodsCommodityCodeUpdatePage

class CommodityCodeUpdatePageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {
  "clean up" - {

    "removes HasCorrectGoodsCommodityCodeUpdatePage when commodity code has changed" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(CommodityCodeUpdatePage(testRecordId), "123")
        .success
        .value
        .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
        .success
        .value

      val result = userAnswers.set(CommodityCodeUpdatePage(testRecordId), "1234").success.value

      result.isDefined(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId)) mustBe false

    }

    "retains HasCorrectGoodsCommodityCodeUpdatePage when commodity code has not changed" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(CommodityCodeUpdatePage(testRecordId), "123")
        .success
        .value
        .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
        .success
        .value

      val result = userAnswers.set(CommodityCodeUpdatePage(testRecordId), "123").success.value

      result.isDefined(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId)) mustBe true
    }
  }
}
