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

package viewmodels.checkAnswers.profile

import models.{CheckMode, NormalMode, UserAnswers}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import controllers.profile.nirms.routes._
import pages.profile.nirms.{NirmsNumberPage, NirmsNumberUpdatePage}

object NirmsNumberSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NirmsNumberPage).map { answer =>
      SummaryListRowViewModel(
        key = "nirmsNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            CreateNirmsNumberController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("nirmsNumber.change.hidden"))
        )
      )
    }

  def row(value: Option[String])(implicit messages: Messages): Option[SummaryListRow] =
    value.map { nirmsNumber =>
      SummaryListRowViewModel(
        key = "nirmsNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(nirmsNumber).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            UpdateNirmsNumberController.onPageLoad(NormalMode).url
          )
            .withVisuallyHiddenText(messages("nirmsNumber.change.hidden"))
        )
      )
    }

  def rowUpdate(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NirmsNumberUpdatePage).map { nirmsNumber =>
      SummaryListRowViewModel(
        key = "nirmsNumber.checkYourAnswersLabel",
        value = ValueViewModel(HtmlFormat.escape(nirmsNumber).toString),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            UpdateNirmsNumberController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("nirmsNumber.change.hidden"))
        )
      )
    }
}
