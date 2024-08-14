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
import models.router.responses.GetGoodsRecordResponse
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

object CategorySummary {

  //TBD - this will be updated to route to the update trader reference page
  def row(record: GetGoodsRecordResponse, recordLocked: Boolean)(implicit
    messages: Messages
  ): SummaryListRow = {
    val categoryValue = getCategoryValue(record)
    if (record.category.isDefined) {
      val action =
        if (recordLocked) { Seq.empty }
        else if (isCommCodeExpired(record.comcodeEffectiveToDate)) {
          Seq(
            ActionItemViewModel(
              "site.change",
              routes.ExpiredCommodityCodeController.onPageLoad(record.recordId).url
            )
              .withVisuallyHiddenText(messages("singleRecord.category.row"))
          )
        } else {
          Seq(
            ActionItemViewModel("site.change", routes.CategoryGuidanceController.onPageLoad(record.recordId).url)
              .withVisuallyHiddenText(messages("singleRecord.category.row"))
          )
        }
      SummaryListRowViewModel(
        key = "singleRecord.category.row",
        value = ValueViewModel(HtmlFormat.escape(messages(categoryValue)).toString),
        actions = action
      )
    } else {
      val translatedValue = messages(categoryValue)
      val viewModel       =
        if (recordLocked) {
          ValueViewModel(HtmlFormat.escape(categoryValue).toString)
        } else if (isCommCodeExpired(record.comcodeEffectiveToDate)) {
          ValueViewModel(
            HtmlContent(
              s"<a href=${routes.ExpiredCommodityCodeController.onPageLoad(record.recordId).url} class='govuk-link'>$translatedValue</a>"
            )
          )
        } else {
          ValueViewModel(
            HtmlContent(
              s"<a href=${routes.CategoryGuidanceController.onPageLoad(record.recordId).url} class='govuk-link'>$translatedValue</a>"
            )
          )
        }
      SummaryListRowViewModel(
        key = "singleRecord.category.row",
        value = viewModel
      )
    }
  }

  private def isCommCodeExpired(commcodeEffectiveToDate: Option[Instant]): Boolean = {
    val today: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
    commcodeEffectiveToDate.exists { effectiveToDate =>
      val effectiveDate: ZonedDateTime = effectiveToDate.atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
      effectiveDate.isEqual(today)
    }
  }

  private def getCategoryValue(record: GetGoodsRecordResponse): String = record.category match {
    case None        => "singleRecord.categoriseThisGood"
    case Some(value) =>
      value match {
        case 1 => "singleRecord.cat1"
        case 2 => "singleRecord.cat2"
        case 3 => "singleRecord.standardGoods"
      }
  }

}
