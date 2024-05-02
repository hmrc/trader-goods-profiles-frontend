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
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

class CheckYourAnswersHelper {

  private def createSummaryListRow(href: String, key: String, value: String): SummaryListRow = SummaryListRow(
    key = Key(HtmlContent(key)),
    value = Value(HtmlContent(value)),
    actions = Some(Actions(items = Seq(ActionItem(href, HtmlContent("Change")))))
  )

  private def createOptionalSummaryListRow(href: String, key: String, value: Option[String]): Option[SummaryListRow] =
    if (value.isEmpty) {
      None
    } else {
      Some(createSummaryListRow(href, key, value.get))
    }

  private def createOptionalYesNoSummaryListRow(
    href: String,
    key: String,
    value: Option[Boolean]
  ): Option[SummaryListRow] = value.map { booleanValue =>
    createSummaryListRow(href, key, if (booleanValue) "Yes" else "No")
  }

  def createSummaryList(traderGoodsProfile: TraderGoodsProfile): List[SummaryListRow] =
    List(
      createOptionalSummaryListRow(
        routes.UkimsNumberController.onPageLoad(CheckMode).url,
        "UKIMS number",
        traderGoodsProfile.ukimsNumber.map(_.value)
      ),
      createOptionalYesNoSummaryListRow(
        routes.NirmsQuestionController.onPageLoad(CheckMode).url,
        "NIRMS registered",
        traderGoodsProfile.hasNirms
      ),
      createOptionalSummaryListRow(
        routes.NirmsNumberController.onPageLoad(CheckMode).url,
        "NIRMS number",
        traderGoodsProfile.nirmsNumber.map(_.value)
      ),
      createOptionalYesNoSummaryListRow(
        routes.NiphlQuestionController.onPageLoad(CheckMode).url,
        "NIPHL registered",
        traderGoodsProfile.hasNiphl
      ),
      createOptionalSummaryListRow(
        routes.NiphlNumberController.onPageLoad(CheckMode).url,
        "NIPHL number",
        traderGoodsProfile.niphlNumber.map(_.value)
      )
    ).flatten
}
