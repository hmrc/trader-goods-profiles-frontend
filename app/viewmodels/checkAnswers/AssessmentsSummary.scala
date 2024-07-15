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
import models.AssessmentAnswer.NoExemption
import models.ott.CategoryAssessment
import models.{CheckMode, UserAnswers}
import pages.AssessmentPage
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.AssessmentCyaKey
object AssessmentsSummary {

  def row(
    recordId: String,
    answers: UserAnswers,
    assessment: CategoryAssessment,
    indexOfThisAssessment: Int,
    numberOfAssessments: Int
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AssessmentPage(recordId, indexOfThisAssessment)).flatMap { answer =>
      val descriptiveText = if (answer == NoExemption) {
        Some("assessment.exemption.none.checkYourAnswersLabel")
      } else {
        val x = assessment.exemptions
          .map(x => x.code)
        Some(x.mkString("", " ", ""))
      }

      val listItems = assessment.exemptions
          .map(x => x.code)

      descriptiveText.map { description =>
        SummaryListRowViewModel(
          key = KeyViewModel(AssessmentCyaKey(listItems).content),
          value = ValueViewModel(
            if(answer.toString == "true") {
              "Yes"
            } else {
              "No"
            }
          ),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              routes.AssessmentController.onPageLoad(CheckMode, recordId, indexOfThisAssessment).url
            )
              .withVisuallyHiddenText(messages("assessment.change.hidden"))
          )
        )
      }
    }
}
