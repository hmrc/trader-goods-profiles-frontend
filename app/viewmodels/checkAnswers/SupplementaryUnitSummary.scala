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
import pages.SupplementaryUnitPage
import play.api.i18n.Messages
import queries.RecordCategorisationsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SupplementaryUnitSummary {

  def row(answers: UserAnswers, recordId: String)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      recordCategorisations <- answers.get(RecordCategorisationsQuery)
      categorisationInfo    <- recordCategorisations.records.get(recordId)
      supplementaryUnit     <- answers.get(SupplementaryUnitPage(recordId))
    } yield {
      val measurementUnit = categorisationInfo.measurementUnit
      val value           = if (measurementUnit.nonEmpty) s"$supplementaryUnit ${measurementUnit.get.trim}" else supplementaryUnit
      SummaryListRowViewModel(
        key = "supplementaryUnit.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.SupplementaryUnitController.onPageLoad(CheckMode, recordId).url)
            .withVisuallyHiddenText(messages("supplementaryUnit.change.hidden"))
        )
      )
    }

  def row(suppValue: Option[Double], measureValue: Option[String], recordId: String)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    for {
      suppUnit <- suppValue
    } yield {
      val displayValue =
        if (measureValue.nonEmpty) s"$suppUnit ${measureValue.get.trim}" else suppUnit.toString()
      SummaryListRowViewModel(
        key = "supplementaryUnit.checkYourAnswersLabel",
        value = ValueViewModel(displayValue),
        actions = Seq(
          ActionItemViewModel("site.change", routes.SupplementaryUnitController.onPageLoad(NormalMode, recordId).url)
            .withVisuallyHiddenText(messages("supplementaryUnit.change.hidden"))
        )
      )
    }
}
