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
import models.DeclarableStatus.{ImmiReady, NotReadyForUse}
import models.ReviewReason.{Inadequate, Mismatch, Unclear}
import models.{CheckMode, NormalMode, UserAnswers}
import pages.goodsRecord.GoodsDescriptionPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import viewmodels.checkAnswers.goodsRecord.GoodsDescriptionSummary

class GoodsDescriptionSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  ".rowUpdate" - {
    "must return a SummaryListRow without change link when record is locked" in {
      val row =
        GoodsDescriptionSummary.rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordLocked = true)
      row.actions mustBe Some(Actions("", List()))
    }

    "must return a SummaryListRow with change link when record is not locked" - {
      "and advice has not been provided" in {
        val row =
          GoodsDescriptionSummary.rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordLocked = false)

        row.actions mustBe defined
        row.actions.value.items.head.href mustEqual controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
          .onPageLoad(NormalMode, testRecordId)
          .url
      }

      "and advice has been provided" in {
        val recordWithAdviceProvided = recordForTestingSummaryRows.copy(adviceStatus = AdviceReceived)
        val row                      =
          GoodsDescriptionSummary.rowUpdate(recordWithAdviceProvided, testRecordId, NormalMode, recordLocked = false)

        row.actions mustBe defined
        row.actions.value.items.head.href mustEqual
          controllers.goodsRecord.goodsDescription.routes.HasGoodsDescriptionChangeController
            .onPageLoad(NormalMode, testRecordId)
            .url
      }

      "must render a 'Does not match' tag when reviewReason is Mismatch and declarable is NotReadyForUse" in {
        val record = recordForTestingSummaryRows.copy(reviewReason = Some(Mismatch), declarable = NotReadyForUse)
        val row    = GoodsDescriptionSummary.rowUpdate(record, testRecordId, NormalMode, recordLocked = false)

        row.value.content.toString must include("""<strong class="govuk-tag govuk-tag--grey">""")
        row.value.content.toString must include(messages("goodsDescription.doesNotMatch"))
        row.value.content.toString must include(record.goodsDescription)
      }

      "must render a 'Not clear' tag when reviewReason is Unclear and declarable is NotReadyForUse" in {
        val record = recordForTestingSummaryRows.copy(reviewReason = Some(Unclear), declarable = NotReadyForUse)
        val row    = GoodsDescriptionSummary.rowUpdate(record, testRecordId, NormalMode, recordLocked = false)

        row.value.content.toString must include("""<strong class="govuk-tag govuk-tag--grey">""")
        row.value.content.toString must include(messages("goodsDescription.unclear"))
        row.value.content.toString must include(record.goodsDescription)
      }

      "must not render a tag if declarable is ImmiReady, even if reviewReason is present" in {
        val record = recordForTestingSummaryRows.copy(reviewReason = Some(Inadequate), declarable = ImmiReady)
        val row    = GoodsDescriptionSummary.rowUpdate(record, testRecordId, NormalMode, recordLocked = false)

        row.value.content.toString must not include "govuk-tag govuk-tag--grey"
        row.value.content.toString must include(record.goodsDescription)
      }
    }
  }

  ".row" - {
    "must return a SummaryListRow when GoodsDescriptionPage is defined" in {
      val ua     = UserAnswers("id").set(GoodsDescriptionPage, "Test").success.value
      val result = GoodsDescriptionSummary.row(ua)

      result mustBe defined
      result.value.key.content.toString   must include(messages("goodsDescription.checkYourAnswersLabel"))
      result.value.value.content.toString must include("Test")
      result.value.actions.value.items.head.href mustEqual
        controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onPageLoad(CheckMode).url
    }

    "must return None when GoodsDescriptionPage is undefined" in {
      val ua     = UserAnswers("id")
      val result = GoodsDescriptionSummary.row(ua)

      result mustBe None
    }
  }

  ".rowUpdateCya" - {
    "must return a SummaryListRow with correct value and link" in {
      val result = GoodsDescriptionSummary.rowUpdateCya("Updated goods", testRecordId, NormalMode)

      result.key.content.toString   must include(messages("goodsDescription.checkYourAnswersLabel"))
      result.value.content.toString must include("Updated goods")
      result.actions.value.items.head.href mustEqual
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
          .onPageLoad(NormalMode, testRecordId)
          .url
    }
  }
}
