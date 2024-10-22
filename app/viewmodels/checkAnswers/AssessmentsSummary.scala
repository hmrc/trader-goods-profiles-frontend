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
import models.ott.CategoryAssessment
import models.{AssessmentAnswer, CheckMode, UserAnswers}
import pages.{AssessmentPage, ReassessmentPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.{AssessmentCyaKey, AssessmentCyaValue}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
object AssessmentsSummary {

  def row(
    recordId: String,
    answers: UserAnswers,
    assessment: CategoryAssessment,
    indexOfThisAssessment: Int,
    isReassessmentAnswer: Boolean
  )(implicit messages: Messages): Option[SummaryListRow] = {

    def createSummaryListRowHelper(answer: AssessmentAnswer, changeLink: String): SummaryListRow = {
      val codes        = assessment.exemptions.map(_.code)
      val descriptions = assessment.exemptions.map(_.description)

      SummaryListRowViewModel(
        key = KeyViewModel(AssessmentCyaKey(codes, descriptions, (indexOfThisAssessment + 1).toString).content),
        value = ValueViewModel(AssessmentCyaValue(codes, descriptions).content).withCssClass("govuk-!-width-one-half"),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            changeLink
          ).withVisuallyHiddenText(messages("assessment.change.hidden", indexOfThisAssessment + 1))
        )
      )
    }

    if (isReassessmentAnswer) {
      answers.get(ReassessmentPage(recordId, indexOfThisAssessment)).map { answer =>
        createSummaryListRowHelper(
          answer.answer,
          routes.AssessmentController.onPageLoadReassessment(CheckMode, recordId, indexOfThisAssessment + 1).url
        )
      }
    } else {
      answers.get(AssessmentPage(recordId, indexOfThisAssessment)).map { answer =>
        createSummaryListRowHelper(
          answer,
          routes.AssessmentController.onPageLoad(CheckMode, recordId, indexOfThisAssessment + 1).url
        )
      }
    }
  }
}
