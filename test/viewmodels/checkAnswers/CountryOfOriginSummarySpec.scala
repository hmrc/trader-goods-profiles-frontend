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
import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, NormalMode}
import play.api.i18n.Messages
import utils.Constants.adviceProvided

import java.time.Instant

class CountryOfOriginSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  private val getGoodsRecordResponse = goodsRecordResponse()
  private val goodsRecordNoCategory = getGoodsRecordResponse.copy(
    category = 1
  )
  private val goodsRecordCategory = getGoodsRecordResponse.copy(
    category = 2
  )

  ".rowUpdate" - {

    "link to country of origin update" - {

      "when categorisation is not done" in {

        val result = GoodsDescriptionSummary.rowUpdate(goodsRecordNoCategory, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.CountryOfOriginController.onPageLoadUpdate(NormalMode, testRecordId).url
        ) mustBe true
      }

    }

    "link to warning page" - {

      "when category is set" in {

        val result = GoodsDescriptionSummary.rowUpdate(goodsRecordCategory, testRecordId, NormalMode)

        result.actions.get.items.exists(p =>
          p.href == routes.HasCountryOfOriginChangeController.onPageLoad(NormalMode, testRecordId).url
        ) mustBe true
      }

    }
  }

}

