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

package viewmodels.checkAnswers

import base.SpecBase
import base.TestConstants.testRecordId
import controllers.routes
import models.NormalMode
import play.api.i18n.Messages
import utils.Constants.adviceProvided

class CommodityCodeSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  private val getGoodsRecordResponse   = goodsRecordResponse()
  private val goodsRecordNoCatNoAdvice = getGoodsRecordResponse.copy(
    category = None,
    adviceStatus = "Not requested"
  )
  private val goodsRecordCatNoAdvice   = getGoodsRecordResponse.copy(
    category = Some(2),
    adviceStatus = "Not requested"
  )
  private val goodsRecordNoCatAdvice   = getGoodsRecordResponse.copy(
    category = None,
    adviceStatus = adviceProvided
  )
  private val goodsRecordCatAdvice     = getGoodsRecordResponse.copy(
    category = Some(2),
    adviceStatus = adviceProvided
  )

  ".rowUpdate" - {

    "link to commodity code update" - {

      "when category is not defined and advice has not been provided" in {

        val result = CommodityCodeSummary.rowUpdate(goodsRecordNoCatNoAdvice, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.CommodityCodeController.onPageLoadUpdate(NormalMode, testRecordId).url
        ) mustBe true
      }

    }

    "link to warning page" - {

      "when category is defined but advice has not been provided" in {

        val result = CommodityCodeSummary.rowUpdate(goodsRecordCatNoAdvice, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.HasCommodityCodeChangeController.onPageLoad(NormalMode, testRecordId).url
        ) mustBe true
      }

      "when category is not defined but advice is provided" in {

        val result = CommodityCodeSummary.rowUpdate(goodsRecordNoCatAdvice, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.HasCommodityCodeChangeController.onPageLoad(NormalMode, testRecordId).url
        ) mustBe true
      }

      "when category is defined and advice is provided" in {

        val result = CommodityCodeSummary.rowUpdate(goodsRecordCatAdvice, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.HasCommodityCodeChangeController.onPageLoad(NormalMode, testRecordId).url
        ) mustBe true
      }

    }
  }

}
