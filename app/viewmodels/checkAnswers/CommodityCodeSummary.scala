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

import controllers.routes
import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, Mode, UserAnswers}
import pages.CommodityCodePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants.adviceProvided
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CommodityCodeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CommodityCodePage).map { answer =>
      SummaryListRowViewModel(
        key = "commodityCode.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CommodityCodeController.onPageLoadCreate(CheckMode).url)
            .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
        )
      )
    }

  def rowUpdateCya(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow = {
    val changeLink = routes.CommodityCodeController.onPageLoadUpdate(mode, recordId).url
    SummaryListRowViewModel(
      key = "commodityCode.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel("site.change", changeLink)
          .withVisuallyHiddenText(messages("commodityCode.change.hidden"))
      )
    )
  }

  def rowUpdate(record: GetGoodsRecordResponse, recordId: String, mode: Mode, recordLocked: Boolean)(implicit
    messages: Messages
  ): SummaryListRow = {
    val changeLink = if (record.category.isDefined || record.adviceStatus == adviceProvided) {
      routes.HasCommodityCodeChangeController.onPageLoad(mode, recordId).url
    } else {
      routes.CommodityCodeController.onPageLoadUpdate(mode, recordId).url
    }

    SummaryListRowViewModel(
      key = "commodityCode.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(record.comcode).toString),
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
