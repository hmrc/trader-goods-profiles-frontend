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
import models.{CheckMode, Country, Mode, NormalMode, UserAnswers}
import pages.CountryOfOriginPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CountryOfOriginSummary {

  def row(answers: UserAnswers, countries: Seq[Country])(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CountryOfOriginPage).map { answer =>
      val description = countries.find(country => country.id == answer).map(_.description).getOrElse(answer)
      SummaryListRowViewModel(
        key = "countryOfOrigin.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(description).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CountryOfOriginController.onPageLoadCreate(CheckMode).url)
            .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
        )
      )
    }

  //TBD - this will be updated to route to the update trader reference page
  def row(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow = {
    val changeLink = mode match {
      case NormalMode => routes.HasCountryOfOriginChangeController.onPageLoad(mode, recordId).url
      case CheckMode  => routes.CountryOfOriginController.onPageLoadUpdate(mode, recordId).url
    }
    SummaryListRowViewModel(
      key = "countryOfOrigin.checkYourAnswersLabel",
      value = ValueViewModel(HtmlFormat.escape(value).toString),
      actions = Seq(
        ActionItemViewModel("site.change", changeLink)
          .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
      )
    )
  }
}
