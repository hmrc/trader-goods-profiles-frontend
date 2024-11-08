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

import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, NormalMode, UserAnswers}
import pages.categorisation.{HasSupplementaryUnitPage, HasSupplementaryUnitUpdatePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Constants.Category2AsInt
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
          ActionItemViewModel(
            "site.change",
            controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, recordId).url
          )
            .withVisuallyHiddenText(messages("hasSupplementaryUnit.change.hidden"))
        )
      )
    }

  def rowUpdate(answers: UserAnswers, recordId: String)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(HasSupplementaryUnitUpdatePage(recordId)).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "hasSupplementaryUnit.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoadUpdate(CheckMode, recordId).url
          )
            .withVisuallyHiddenText(messages("hasSupplementaryUnit.change.hidden"))
        )
      )
    }

  def row(record: GetGoodsRecordResponse, recordId: String, recordLocked: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    if (record.category.contains(Category2AsInt)) {
      for {
        _ <- record.measurementUnit
      } yield {
        val displayValue = record.supplementaryUnit match {
          case Some(value) if value != 0 => "site.yes"
          case _                         => "site.no"
        }
        SummaryListRowViewModel(
          key = "hasSupplementaryUnit.checkYourAnswersLabel",
          value = ValueViewModel(displayValue),
          actions = if (recordLocked) {
            Seq.empty
          } else {
            Seq(
              ActionItemViewModel(
                "site.change",
                controllers.categorisation.routes.HasSupplementaryUnitController
                  .onPageLoadUpdate(NormalMode, recordId)
                  .url
              )
                .withVisuallyHiddenText(messages("hasSupplementaryUnit.change.hidden"))
            )
          }
        )
      }
    } else {
      None
    }
}
