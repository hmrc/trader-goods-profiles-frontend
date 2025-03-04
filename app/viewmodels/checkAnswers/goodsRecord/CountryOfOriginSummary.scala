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

package viewmodels.checkAnswers.goodsRecord

import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, Country, Mode, UserAnswers}
import pages.goodsRecord.CountryOfOriginPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CountryOfOriginSummary {

  def row(answers: UserAnswers, countries: Seq[Country])(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CountryOfOriginPage).map { answer =>
      val country = countries.find(country => country.id == answer).map(_.description).getOrElse(answer)
      SummaryListRowViewModel(
        key = "countryOfOrigin.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(s"<div lang='en'>${HtmlFormat.escape(country).toString}</div>")),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
        )
      )
    }

  def rowUpdateCya(value: String, recordId: String, mode: Mode)(implicit messages: Messages): SummaryListRow = {
    val changeLink =
      controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController.onPageLoad(mode, recordId).url
    SummaryListRowViewModel(
      key = "countryOfOrigin.checkYourAnswersLabel",
      value = ValueViewModel(HtmlContent(s"<div lang='en'>${HtmlFormat.escape(value).toString}</div>")),
      actions = Seq(
        ActionItemViewModel("site.change", changeLink)
          .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
      )
    )
  }

  def rowUpdate(
    record: GetGoodsRecordResponse,
    recordId: String,
    mode: Mode,
    recordLocked: Boolean,
    countries: Seq[Country]
  )(implicit
    messages: Messages
  ): SummaryListRow = {

    val countryName = getCountryName(record.countryOfOrigin, countries)

    val changeLink = if (record.category.isDefined) {
      controllers.goodsRecord.countryOfOrigin.routes.HasCountryOfOriginChangeController.onPageLoad(mode, recordId).url
    } else {
      controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController.onPageLoad(mode, recordId).url
    }
    SummaryListRowViewModel(
      key = "countryOfOrigin.checkYourAnswersLabel",
      value = ValueViewModel(HtmlContent(s"<div lang='en'>${HtmlFormat.escape(countryName).toString}</div>")),
      actions = if (recordLocked) {
        Seq.empty
      } else {
        Seq(
          ActionItemViewModel("site.change", changeLink)
            .withVisuallyHiddenText(messages("countryOfOrigin.change.hidden"))
        )
      }
    )
  }

  private def getCountryName(countryOfOrigin: String, countries: Seq[Country]) =
    if (countries.isEmpty) {
      countryOfOrigin
    } else {
      countries.find(country => country.id == countryOfOrigin).map(_.description).getOrElse(countryOfOrigin)
    }

}
