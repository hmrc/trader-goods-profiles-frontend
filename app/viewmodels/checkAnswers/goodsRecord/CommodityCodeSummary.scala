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

package viewmodels.checkAnswers.goodsRecord

import models.AdviceStatus.AdviceReceived
import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, Mode, UserAnswers}
import pages.goodsRecord.CommodityCodePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CommodityCodeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CommodityCodePage).map { answer =>
      SummaryListRowViewModel(
        key = "commodityCode.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
        )
      )
    }

  def rowUpdateCya(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow = {
    val changeLink =
      controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(mode, recordId).url
    SummaryListRowViewModel(
      key = "commodityCode.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel("site.change", changeLink)
          .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
      )
    )
  }

  def rowUpdate(
    record: GetGoodsRecordResponse,
    recordId: String,
    mode: Mode,
    recordLocked: Boolean,
    isReviewReasonCommodity: Boolean
  )(implicit
    messages: Messages
  ): SummaryListRow = {
    val changeLink = if (record.category.isDefined || record.adviceStatus == AdviceReceived) {
      controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController.onPageLoad(mode, recordId).url
    } else {
      controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(mode, recordId).url
    }

    val tagValue  = messages("singleRecord.commodityReviewReason.tagText")
    val viewModel =
      if (isReviewReasonCommodity) {
        ValueViewModel(
          HtmlContent(
            s"<strong class='govuk-tag govuk-tag--grey'>$tagValue</strong> ${record.comcode}"
          )
        )
      } else {
        ValueViewModel(HtmlFormat.escape(record.comcode).toString)
      }

    SummaryListRowViewModel(
      key = "commodityCode.checkYourAnswersLabel",
      value = viewModel,
      actions = if (recordLocked) {
        Seq.empty
      } else {
        Seq(
          ActionItemViewModel("site.change", changeLink)
            .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
        )
      }
    )
  }
}
