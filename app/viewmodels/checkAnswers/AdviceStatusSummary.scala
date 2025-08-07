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

import models.AdviceStatus
import models.AdviceStatus.AdviceReceived
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AdviceStatusSummary {

  def row(adviceStatus: AdviceStatus, recordId: String, recordLocked: Boolean, isReviewReasonCommodity: Boolean)(
    implicit messages: Messages
  ): SummaryListRow = {

    val tagValue = messages("singleRecord.reviewReason.tagText")

    val viewModel =
      if (isReviewReasonCommodity && adviceStatus == AdviceReceived) {
        ValueViewModel(
          HtmlContent(
            s"<strong class='govuk-tag govuk-tag--grey'>$tagValue</strong> ${messages(adviceStatus.messageKey)}"
          )
        )
      } else {
        ValueViewModel(HtmlFormat.escape(messages(adviceStatus.messageKey)).toString)
      }

    SummaryListRowViewModel(
      key = "singleRecord.adviceStatus.row",
      value = viewModel,
      actions = if (adviceStatus == AdviceReceived) {
        Seq.empty
      } else if (recordLocked) {
        Seq(
          ActionItemViewModel(
            "singleRecord.withdrawAdvice",
            controllers.advice.routes.WithdrawAdviceStartController.onPageLoad(recordId).url
          )
        )
      } else {
        Seq(
          ActionItemViewModel(
            "singleRecord.askForAdvice",
            controllers.advice.routes.AdviceStartController.onPageLoad(recordId).url
          )
        )
      }
    )
  }
}
