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
import models.requests.DataRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

class CheckYourAnswersHelper {

  def createSummaryListRow(href: String, key: String, value: String): SummaryListRow = SummaryListRow(
    key = Key(HtmlContent(key)),
    value = Value(HtmlContent(value)),
    actions = Some(Actions(items = Seq(ActionItem(href, HtmlContent("Change")))))
  )

  def createOptionalSummaryListRow(href: String, key: String, value: Option[Any]): Option[SummaryListRow] =
    if (value.isEmpty) {
      None
    } else {
      Some(createSummaryListRow(href, key, value.get.toString))
    }

  def createOptionalYesNoSummaryListRow(href: String, key: String, value: Option[Boolean]): Option[SummaryListRow] =
    if (value.isEmpty) {
      None
    } else {
      Some(createSummaryListRow(href, key, if (value.get) "Yes" else "No"))
    }

  def createSummaryList(request: DataRequest[AnyContent]): List[SummaryListRow] =
    List(
      createOptionalSummaryListRow(
        routes.UkimsNumberController.onPageLoad.url,
        "UKIMS number",
        request.userAnswers.traderGoodsProfile.flatMap(_.ukimsNumber)
      ),
      createOptionalYesNoSummaryListRow(
        routes.NirmsQuestionController.onPageLoad.url,
        "NIRMS registered",
        request.userAnswers.traderGoodsProfile.flatMap(_.hasNirms)
      ),
      createOptionalSummaryListRow(
        routes.NirmsNumberController.onPageLoad.url,
        "NIRMS number",
        request.userAnswers.traderGoodsProfile.flatMap(_.nirmsNumber)
      ),
      createOptionalYesNoSummaryListRow(
        routes.NiphlQuestionController.onPageLoad.url,
        "NIPHL registered",
        request.userAnswers.traderGoodsProfile.flatMap(_.hasNiphl)
      ),
      createOptionalSummaryListRow(
        routes.NiphlNumberController.onPageLoad.url,
        "NIPHL number",
        request.userAnswers.traderGoodsProfile.flatMap(_.niphlNumber)
      )
    ).flatten
}
