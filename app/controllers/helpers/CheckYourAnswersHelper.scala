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

package controllers.helpers

import controllers.routes
import models.{CheckMode, TraderGoodsProfile}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

class CheckYourAnswersHelper() {
  private def createSummaryListRow(key: String, href: String, value: Option[Any])(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    val changeText = messages("site.change")
    value.flatMap {
      case stringValue: String   =>
        Some(
          SummaryListRow(
            key = Key(HtmlContent(key)),
            value = Value(HtmlContent(stringValue)),
            actions = Some(Actions(items = Seq(ActionItem(href, HtmlContent(changeText)))))
          )
        )
      case booleanValue: Boolean =>
        Some(
          SummaryListRow(
            key = Key(HtmlContent(key)),
            value = Value(HtmlContent(if (booleanValue) "Yes" else "No")),
            actions = Some(Actions(items = Seq(ActionItem(href, HtmlContent(changeText)))))
          )
        )
      case _                     => None
    }
  }
  def createSummaryList(traderGoodsProfile: TraderGoodsProfile)(implicit messages: Messages): List[SummaryListRow] = {
    val summaryData: Seq[(String, String, Option[Any])] = Seq(
      (
        "UKIMS number",
        routes.UkimsNumberController.onPageLoad(CheckMode).url,
        traderGoodsProfile.ukimsNumber.map(_.value)
      ),
      (
        "NIRMS registered",
        routes.NirmsQuestionController.onPageLoad(CheckMode).url,
        traderGoodsProfile.hasNirms
      ),
      (
        "NIRMS number",
        routes.NirmsNumberController.onPageLoad(CheckMode).url,
        traderGoodsProfile.nirmsNumber.map(_.value)
      ),
      (
        "NIPHL registered",
        routes.NiphlQuestionController.onPageLoad(CheckMode).url,
        traderGoodsProfile.hasNiphl
      ),
      (
        "NIPHL number",
        routes.NiphlNumberController.onPageLoad(CheckMode).url,
        traderGoodsProfile.niphlNumber.map(_.value)
      )
    )

    summaryData
      .map { case (key, url, value) =>
        createSummaryListRow(key, url, value)
      }
      .flatten
      .toList
  }

}
