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

package viewmodels.checkAnswers.profile

import models.{CheckMode, Mode, UserAnswers}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import controllers.profile.niphl.routes._
import pages.profile.niphl.{HasNiphlPage, HasNiphlUpdatePage}

object HasNiphlSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasNiphlPage).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "hasNiphl.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            HasNiphlController.onPageLoadCreate(CheckMode).url
          )
            .withVisuallyHiddenText(messages("hasNiphl.change.hidden"))
        )
      )
    }

  def row(value: Boolean, mode: Mode)(implicit messages: Messages): SummaryListRow = {
    val textValue = if (value) "site.yes" else "site.no"
    SummaryListRowViewModel(
      key = "hasNiphl.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(textValue).toString),
      actions = Seq(
        ActionItemViewModel("site.change", HasNiphlController.onPageLoadUpdate(mode).url)
          .withVisuallyHiddenText(messages("hasNiphl.change.hidden"))
      )
    )
  }

  def rowUpdate(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasNiphlUpdatePage).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "hasNiphl.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            HasNiphlController.onPageLoadUpdate(CheckMode).url
          )
            .withVisuallyHiddenText(messages("hasNiphl.change.hidden"))
        )
      )
    }
}
