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
import models.AdviceStatus.AdviceReceived
import models.NormalMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import viewmodels.checkAnswers.goodsRecord.CommodityCodeSummary

class CommodityCodeSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  "must return a SummaryListRow without change links when record is locked" in {

    val row =
      CommodityCodeSummary.rowUpdate(
        recordForTestingSummaryRows,
        testRecordId,
        NormalMode,
        recordLocked = true,
        isReviewReasonCommodity = false
      )

    row.actions mustBe Some(Actions("", List()))
  }

  "must return a SummaryListRow with change links when record is not locked" - {

    "and category is set" in {

      val row =
        CommodityCodeSummary.rowUpdate(
          recordForTestingSummaryRows,
          testRecordId,
          NormalMode,
          recordLocked = false,
          isReviewReasonCommodity = false
        )

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "and advice is provided" in {

      val recordAdviceProvided = recordForTestingSummaryRows.copy(adviceStatus = AdviceReceived)

      val row =
        CommodityCodeSummary.rowUpdate(
          recordAdviceProvided,
          testRecordId,
          NormalMode,
          recordLocked = false,
          isReviewReasonCommodity = false
        )

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "and neither category is set nor advice is provided" in {

      val recordNoCatNoAdvice = recordForTestingSummaryRows.copy(category = None)

      val row =
        CommodityCodeSummary.rowUpdate(
          recordNoCatNoAdvice,
          testRecordId,
          NormalMode,
          recordLocked = false,
          isReviewReasonCommodity = false
        )

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }
  }

}
