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

import models.AdviceStatus.{AdviceNotProvided, AdviceRequestWithdrawn}
import models.DeclarableStatus.NotReadyForUse
import models.ReviewReason.{Inadequate, Mismatch, Unclear}
import models.router.responses.GetGoodsRecordResponse
import models.{AdviceStatus, CheckMode, Mode, UserAnswers}
import pages.goodsRecord.GoodsDescriptionPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object GoodsDescriptionSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(GoodsDescriptionPage).map { answer =>
      SummaryListRowViewModel(
        key = "goodsDescription.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("goodsDescription.change.hidden"))
        )
      )
    }

  def rowUpdateCya(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "goodsDescription.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
            .onPageLoad(mode, recordId)
            .url
        )
          .withVisuallyHiddenText(messages("goodsDescription.change.hidden"))
      )
    )

  def rowUpdate(record: GetGoodsRecordResponse, recordId: String, mode: Mode, recordLocked: Boolean)(implicit
    messages: Messages
  ): SummaryListRow = {
    val changeLink =
      if (
        record.adviceStatus == AdviceStatus.NotRequested || record.adviceStatus == AdviceRequestWithdrawn || record.adviceStatus == AdviceNotProvided
      ) {
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController.onPageLoad(mode, recordId).url
      } else {
        controllers.goodsRecord.goodsDescription.routes.HasGoodsDescriptionChangeController
          .onPageLoad(mode, recordId)
          .url
      }

    SummaryListRowViewModel(
      key = "goodsDescription.checkYourAnswersLabel",
      value = {
        val tagTextOpt: Option[String] = record.reviewReason match {
          case Some(Mismatch) if record.declarable == NotReadyForUse             => Some(messages("goodsDescription.doesNotMatch"))
          case Some(Unclear | Inadequate) if record.declarable == NotReadyForUse =>
            Some(messages("goodsDescription.unclear"))
          case _                                                                 => None
        }

        val tagHtml: String =
          tagTextOpt.map(text => s"""<strong class="govuk-tag govuk-tag--grey">$text</strong> """).getOrElse("")
        val description     = HtmlFormat.escape(record.goodsDescription).toString

        ValueViewModel(HtmlContent(HtmlFormat.raw(tagHtml + description)))
      },
      actions = if (recordLocked) {
        Seq.empty
      } else {
        Seq(
          ActionItemViewModel("site.change", changeLink)
            .withVisuallyHiddenText(messages("goodsDescription.change.hidden"))
        )
      }
    )
  }
}
