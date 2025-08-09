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
import models.DeclarableStatus.NotReadyForUse
import models.NormalMode
import models.ReviewReason.Mismatch
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import viewmodels.checkAnswers.goodsRecord.CommodityCodeSummary

class CommodityCodeSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  "must return a SummaryListRow without change links when record is locked" in {
    val row = CommodityCodeSummary.rowUpdate(
      recordForTestingSummaryRows,
      testRecordId,
      NormalMode,
      recordLocked = true,
      None
    )
    row.actions mustBe Some(Actions("", List()))
  }

  "must return a SummaryListRow with change links when record is not locked" - {
    "and category is set" in {
      val row = CommodityCodeSummary.rowUpdate(
        recordForTestingSummaryRows,
        testRecordId,
        NormalMode,
        recordLocked = false,
        None
      )
      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "and advice is provided" in {
      val recordAdviceProvided = recordForTestingSummaryRows.copy(adviceStatus = AdviceReceived)
      val row                  = CommodityCodeSummary.rowUpdate(
        recordAdviceProvided,
        testRecordId,
        NormalMode,
        recordLocked = false,
        None
      )
      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "and neither category is set nor advice is provided" in {
      val recordNoCatNoAdvice = recordForTestingSummaryRows.copy(category = None)
      val row                 = CommodityCodeSummary.rowUpdate(
        recordNoCatNoAdvice,
        testRecordId,
        NormalMode,
        recordLocked = false,
        None
      )
      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "must render a 'Does not match' tag when reviewReason is Mismatch and declarable is NotReadyForUse" in {
      val record = recordForTestingSummaryRows.copy(declarable = NotReadyForUse)

      val row = CommodityCodeSummary.rowUpdate(
        record,
        testRecordId,
        NormalMode,
        recordLocked = false,
        Some(Mismatch)
      )

      import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent

      val html = row.value.content match {
        case hc: HtmlContent => hc.value.toString
        case other           => fail(s"Expected HtmlContent but got: $other")
      }

      html must include("""<strong class="govuk-tag govuk-tag--grey""")
      html must include(messages("commodityCode.mismatch"))
      html must include(record.comcode)
    }

  }
}
