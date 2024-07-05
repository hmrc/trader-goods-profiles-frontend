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
import models.{CheckMode, Mode, UserAnswers}
import pages.TraderReferencePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TraderReferenceSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TraderReferencePage).map { answer =>
      SummaryListRowViewModel(
        key = "traderReference.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.TraderReferenceController.onPageLoadCreate(CheckMode).url)
            .withVisuallyHiddenText(messages("traderReference.change.hidden"))
        )
      )
    }

  //TBD - this will be updated to route to the update trader reference page
  def row(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "traderReference.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel("site.change", routes.TraderReferenceController.onPageLoadUpdate(mode, recordId).url)
          .withVisuallyHiddenText(messages("traderReference.change.hidden"))
      )
    )
}
