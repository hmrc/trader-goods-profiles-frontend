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
import models.DeclarableStatus.NotReadyForUse
import models.ReviewReason.Mismatch
import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, Mode, ReviewReason, UserAnswers}
import pages.goodsRecord.CommodityCodePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object CommodityCodeSummary {

  def row(answers: UserAnswers, reviewReason: Option[ReviewReason])(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CommodityCodePage).map { answer =>
      val valueHtml = reviewReason match {
        case Some(ReviewReason.Commodity) =>
          val tagValue = messages("singleRecord.reviewReason.tagText")
          HtmlContent(s"""<div style="display: flex; align-items: center;">
                           <strong class="govuk-tag govuk-tag--grey" style="margin-right: 8px;">$tagValue</strong>
                           ${HtmlFormat.escape(answer).toString}
                         </div>""")
        case _ =>
          HtmlContent(HtmlFormat.escape(answer).toString)
      }

      SummaryListRowViewModel(
        key = "commodityCode.checkYourAnswersLabel",
        value = ValueViewModel(valueHtml),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode).url
          ).withVisuallyHiddenText(messages("commodityCode.change.hidden"))
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
                 reviewReason: Option[ReviewReason]
               )(implicit messages: Messages): SummaryListRow = {

    val changeLink = if (record.category.isDefined || record.adviceStatus == AdviceReceived) {

      controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController.onPageLoad(mode, recordId).url
    } else {
      controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(mode, recordId).url
    }

    val tagHtml: String = (reviewReason, record.declarable) match {
      case (Some(Mismatch), NotReadyForUse) =>
        val tagText = messages("commodityCode.mismatch")
        s"""<strong class="govuk-tag govuk-tag--grey" style="margin-right: 8px;">$tagText</strong>"""
      case (Some(ReviewReason.Commodity), _) =>
        val tagValue = messages("singleRecord.reviewReason.tagText")
        s"""<strong class="govuk-tag govuk-tag--grey" style="margin-right: 8px;">$tagValue</strong>"""
      case _ => ""
    }

    val description = HtmlFormat.escape(record.comcode).toString

    val valueHtml = HtmlContent(
      s"""<div style="display: flex; align-items: center;">$tagHtml$description</div>"""
    )

    SummaryListRowViewModel(
      key = "commodityCode.checkYourAnswersLabel",
      value = ValueViewModel(valueHtml),
      actions = if (recordLocked) Seq.empty else Seq(
        ActionItemViewModel("site.change", changeLink)
          .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
      )
    )
  }


}
