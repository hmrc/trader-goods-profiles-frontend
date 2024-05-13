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

import models.{CheckMode, MaintainProfileAnswers}
import play.api.i18n.Messages
import controllers.routes
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

class CheckYourAnswersHelper() {

  def changeToYesOrNo(boolean: Boolean)(implicit messages: Messages): String =
    if (boolean) messages("site.yes") else messages("site.no")

  private def createSummaryListRow(key: String, href: String, value: Option[String])(implicit
    messages: Messages
  ): Option[SummaryListRow] = value.flatMap { anyValue =>
    Some(
      SummaryListRow(
        key = Key(HtmlContent(key)),
        value = Value(HtmlContent(anyValue)),
        actions = Some(Actions(items = Seq(ActionItem(href, HtmlContent(messages("site.change"))))))
      )
    )
  }
  def createSummaryList(
    maintainProfileAnswers: MaintainProfileAnswers
  )(implicit messages: Messages): Seq[SummaryListRow] = {
    val summaryData: Seq[(String, String, Option[String])] = Seq(
      (
        messages("checkYourAnswers.ukimsNumber"),
        routes.UkimsNumberController.onPageLoad(CheckMode).url,
        maintainProfileAnswers.ukimsNumber.map(_.value)
      ),
      (
        messages("checkYourAnswers.nirmsRegistered"),
        routes.NirmsQuestionController.onPageLoad(CheckMode).url,
        maintainProfileAnswers.hasNirms.map(changeToYesOrNo)
      ),
      (
        messages("checkYourAnswers.nirmsNumber"),
        routes.NirmsNumberController.onPageLoad(CheckMode).url,
        maintainProfileAnswers.nirmsNumber.map(_.value)
      ),
      (
        messages("checkYourAnswers.niphlRegistered"),
        routes.NiphlQuestionController.onPageLoad(CheckMode).url,
        maintainProfileAnswers.hasNiphl.map(changeToYesOrNo)
      ),
      (
        messages("checkYourAnswers.niphlNumber"),
        routes.NiphlNumberController.onPageLoad(CheckMode).url,
        maintainProfileAnswers.niphlNumber.map(_.value)
      )
    )

    summaryData.flatMap { case (key, url, value) =>
      createSummaryListRow(key, url, value)
    }
  }

}
