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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CategorySummary {

  //TBD - this will be updated to route to the update trader reference page
  def row(value: String, recordId: String, isCategorised: Boolean)(implicit messages: Messages): SummaryListRow = {

    val linkText = if (isCategorised) "site.change" else "singleRecord.categoriseThisGood"
    SummaryListRowViewModel(
      key = "singleRecord.category.row",
      value = ValueViewModel(HtmlFormat.escape(messages(value)).toString),
      actions = Seq(
        ActionItemViewModel(linkText, routes.CategoryGuidanceController.onPageLoad(recordId).url)
          .withVisuallyHiddenText(messages("singleRecord.category.row"))
      )
    )
  }
}
