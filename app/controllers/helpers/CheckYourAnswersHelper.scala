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
import models.TraderGoodsProfile
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
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
  ): Option[SummaryListRow] =
    if (value.isEmpty) {
      None
    } else {
      Some(createSummaryListRow(href, key, if (value.get) "Yes" else "No"))
    }

  def redirectToCheckYourAnswersIfNeeded(traderGoodsProfile: TraderGoodsProfile, alternativeHref: String): Result = if (
    traderGoodsProfile.ukimsNumber.isDefined
    && traderGoodsProfile.hasNirms.isDefined
    && traderGoodsProfile.hasNiphl.isDefined
  ) {
    Redirect(routes.CheckYourAnswersController.onPageLoad.url)
  } else {
    Redirect(alternativeHref)
  }
  def getAppropriateUrl(traderGoodsProfile: TraderGoodsProfile, alternativeHref: String): String                  = if (
    traderGoodsProfile.ukimsNumber.isDefined
    && traderGoodsProfile.hasNirms.isDefined
    && traderGoodsProfile.hasNiphl.isDefined
  ) {
    routes.CheckYourAnswersController.onPageLoad.url
  } else {
    alternativeHref
  }
  def createSummaryList(traderGoodsProfile: TraderGoodsProfile): List[SummaryListRow]                             =
    List(
      createOptionalSummaryListRow(
        routes.UkimsNumberController.onPageLoad.url,
        "UKIMS number",
        traderGoodsProfile.ukimsNumber match {
          case Some(x) => Some(x.value)
          case None    => None
        }
      ),
      createOptionalYesNoSummaryListRow(
        routes.NirmsQuestionController.onPageLoad.url,
        "NIRMS registered",
        traderGoodsProfile.hasNirms
      ),
      createOptionalSummaryListRow(
        routes.NirmsNumberController.onPageLoad.url,
        "NIRMS number",
        traderGoodsProfile.nirmsNumber match {
          case Some(x) => Some(x.value)
          case None    => None
        }
      ),
      createOptionalYesNoSummaryListRow(
        routes.NiphlQuestionController.onPageLoad.url,
        "NIPHL registered",
        traderGoodsProfile.hasNiphl
      ),
      createOptionalSummaryListRow(
        routes.NiphlNumberController.onPageLoad.url,
        "NIPHL number",
        traderGoodsProfile.niphlNumber match {
          case Some(x) => Some(x.value)
          case None    => None
        }
      )
    ).flatten
}
