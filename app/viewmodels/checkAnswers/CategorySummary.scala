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

import models.ReviewReason
import models.ReviewReason.{Commodity, Measure}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CategorySummary {

  def row(
    value: String,
    recordId: String,
    recordLocked: Boolean,
    isCategorised: Boolean,
    reviewReason: Option[ReviewReason]
  )(implicit
    messages: Messages
  ): SummaryListRow = {
    val url = controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId).url
    if (isCategorised) {
      val action =
        if (recordLocked) {
          Seq.empty
        } else {
          Seq(
            ActionItemViewModel("site.change", url)
              .withVisuallyHiddenText(messages("singleRecord.category.row"))
          )
        }

      val tagValue = messages("singleRecord.commodityReviewReason.tagText")
      if (reviewReason.exists(_ == Measure)) {
        val translatedValue = messages("singleRecord.categoriseThisGood")
        val viewModel       =
          ValueViewModel(
            HtmlContent(
              s"<a href=$url class='govuk-link'>$translatedValue</a>"
            )
          )
        SummaryListRowViewModel(
          key = "singleRecord.category.row",
          value = viewModel
        )
      } else if (reviewReason.exists(_ == Commodity)) {
        val viewModel =
          ValueViewModel(
            HtmlContent(
              s"<strong class='govuk-tag govuk-tag--grey'>$tagValue</strong>$value"
            )
          )
        SummaryListRowViewModel(
          key = "singleRecord.category.row",
          value = viewModel
        )
      } else {
        SummaryListRowViewModel(
          key = "singleRecord.category.row",
          value = ValueViewModel(HtmlFormat.escape(messages(value)).toString),
          actions = action
        )
      }
    } else {
      val viewModel =
        if (recordLocked) {
          ValueViewModel(HtmlFormat.escape(value).toString)
        } else {
          if (reviewReason.exists(_ == Commodity)) {
            val translatedValue = messages("singleRecord.category.row.commodityReviewReason.notCategorised")
            ValueViewModel(HtmlFormat.escape(translatedValue).toString())
          } else {
            val translatedValue = messages(value)
            ValueViewModel(
              HtmlContent(
                s"<a href=$url class='govuk-link'>$translatedValue</a>"
              )
            )
          }
        }
      SummaryListRowViewModel(
        key = "singleRecord.category.row",
        value = viewModel
      )
    }
  }
}
