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
import models.{CheckMode, NormalMode, UserAnswers}
import pages.HasSupplementaryUnitPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object HasSupplementaryUnitSummary {

  def row(answers: UserAnswers, recordId: String)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasSupplementaryUnitPage(recordId)).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "hasSupplementaryUnit.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId).url)
            .withVisuallyHiddenText(messages("hasSupplementaryUnit.change.hidden"))
        )
      )
    }

  def row(value: Boolean, recordId: String)(implicit messages: Messages): Option[SummaryListRow] = {

    val textValue = if (value) "site.yes" else "site.no"
    Some(
      SummaryListRowViewModel(
        key = "hasSupplementaryUnit.checkYourAnswersLabel",
        value = ValueViewModel(textValue),
        actions = Seq(
          ActionItemViewModel("site.change", routes.HasSupplementaryUnitController.onPageLoad(NormalMode, recordId).url)
            .withVisuallyHiddenText(messages("hasSupplementaryUnit.change.hidden"))
        )
      )
    )

  }
}
