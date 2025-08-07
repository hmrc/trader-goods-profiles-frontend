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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryListRow, Value}
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
    val url = createCategorisationUrl(recordId)

    if (isCategorised) {
      handleCategorisedRow(value, url, recordLocked, reviewReason)
    } else {
      handleUncategorisedRow(value, url, recordLocked, reviewReason)
    }
  }

  private def createCategorisationUrl(recordId: String): String =
    controllers.routes.ValidateCommodityCodeController.changeCategory(recordId).url

  private def handleCategorisedRow(
    value: String,
    url: String,
    recordLocked: Boolean,
    reviewReason: Option[ReviewReason]
  )(implicit messages: Messages): SummaryListRow = {
    val actions = createCategorisedActions(recordLocked, url)

    reviewReason match {
      case Some(Measure)   =>
        val viewModel = createMeasureViewModel(url)
        SummaryListRowViewModel(key = "singleRecord.category.row", value = viewModel)
      case Some(Commodity) =>
        val viewModel = createCommodityViewModel(value)
        SummaryListRowViewModel(key = "singleRecord.category.row", value = viewModel)
      case _               =>
        val escapedValue = ValueViewModel(HtmlFormat.escape(messages(value)).toString)
        SummaryListRowViewModel(
          key = "singleRecord.category.row",
          value = escapedValue,
          actions = actions
        )
    }
  }

  private def createCategorisedActions(recordLocked: Boolean, url: String)(implicit
    messages: Messages
  ): Seq[ActionItem] =
    if (recordLocked) {
      Seq.empty
    } else {
      Seq(ActionItemViewModel("site.change", url).withVisuallyHiddenText(messages("singleRecord.category.row")))
    }

  private def createMeasureViewModel(url: String)(implicit messages: Messages): Value = {
    val translatedValue = messages("singleRecord.categoriseThisGood")
    ValueViewModel(HtmlContent(s"<a href=$url class='govuk-link'>$translatedValue</a>"))
  }

  private def createCommodityViewModel(value: String)(implicit messages: Messages): Value = {
    val tagValue = messages("singleRecord.reviewReason.tagText")
    ValueViewModel(HtmlContent(s"<strong class='govuk-tag govuk-tag--grey'>$tagValue</strong> ${messages(value)}"))
  }

  private def handleUncategorisedRow(
    value: String,
    url: String,
    recordLocked: Boolean,
    reviewReason: Option[ReviewReason]
  )(implicit messages: Messages): SummaryListRow = {
    val viewModel = if (recordLocked) {
      ValueViewModel(HtmlFormat.escape(value).toString)
    } else {
      createUncategorisedViewModel(value, url, reviewReason)
    }

    SummaryListRowViewModel(key = "singleRecord.category.row", value = viewModel)
  }

  private def createUncategorisedViewModel(
    value: String,
    url: String,
    reviewReason: Option[ReviewReason]
  )(implicit messages: Messages): Value =
    reviewReason match {
      case Some(Commodity) =>
        val translatedValue = messages("singleRecord.category.row.commodityReviewReason.notCategorised")
        ValueViewModel(HtmlFormat.escape(translatedValue).toString())
      case _               =>
        val translatedValue = messages(value)
        ValueViewModel(HtmlContent(s"<a href=$url class='govuk-link'>$translatedValue</a>"))
    }

}
