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
import models.{CheckMode, Mode, TraderProfile, UserAnswers}
import pages.UkimsNumberPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object UkimsNumberSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(UkimsNumberPage).map { answer =>
      SummaryListRowViewModel(
        key = "ukimsNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.UkimsNumberController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("ukimsNumber.change.hidden"))
        )
      )
    }

  //TBD - this will be updated to route to the update ukims number page
  def row(value: String, mode: Mode)(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = "ukimsNumber.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel("site.change", routes.UkimsNumberController.onPageLoad(mode).url)
          .withVisuallyHiddenText(messages("ukimsNumber.change.hidden"))
      )
    )
}
