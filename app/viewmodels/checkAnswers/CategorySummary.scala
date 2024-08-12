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
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CategorySummary {

  //TBD - this will be updated to route to the update trader reference page
  def row(value: String, recordId: String, recordLocked: Boolean, isCategorised: Boolean)(implicit
    messages: Messages
  ): SummaryListRow =
    if (isCategorised) {
      val action =
        if (recordLocked) Seq.empty
        else {
          Seq(
            ActionItemViewModel("site.change", routes.CategoryGuidanceController.onPageLoad(recordId).url)
              .withVisuallyHiddenText(messages("singleRecord.category.row"))
          )
        }
      SummaryListRowViewModel(
        key = "singleRecord.category.row",
        value = ValueViewModel(HtmlFormat.escape(messages(value)).toString),
        actions = action
      )
    } else {
      val translatedValue = messages(value)
      val viewModel       =
        if (recordLocked) {
          ValueViewModel(HtmlFormat.escape(value).toString)
        } else {
          ValueViewModel(
            HtmlContent(
              s"<a href=${routes.CategoryGuidanceController.onPageLoad(recordId).url} class='govuk-link'>$translatedValue</a>"
            )
          )
        }
      SummaryListRowViewModel(
        key = "singleRecord.category.row",
        value = viewModel
      )
    }
}
