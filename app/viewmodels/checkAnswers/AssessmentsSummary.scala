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
import viewmodels.AssessmentCyaKey
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
object AssessmentsSummary {

  def row(
    recordId: String,
    answers: UserAnswers,
    assessment: CategoryAssessment,
    indexOfThisAssessment: Int,
    reassessmentAnswer: Boolean
  )(implicit messages: Messages): Option[SummaryListRow] = {

    val pageToUse = if (reassessmentAnswer) {
      ReassessmentPage(recordId, indexOfThisAssessment)
    } else {
      AssessmentPage(recordId, indexOfThisAssessment)
    }

    val changeLink = if (reassessmentAnswer) {
      routes.AssessmentController.onPageLoadReassessment(CheckMode, recordId, indexOfThisAssessment).url
    } else {
      routes.AssessmentController.onPageLoad(CheckMode, recordId, indexOfThisAssessment).url
    }

    answers.get(pageToUse).map { answer =>
      val codes        = assessment.exemptions.map(_.code)
      val descriptions = assessment.exemptions.map(_.description)

      SummaryListRowViewModel(
        key = KeyViewModel(AssessmentCyaKey(codes, descriptions, (indexOfThisAssessment + 1).toString).content)
          .withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(
          if (answer == AssessmentAnswer.Exemption) {
            messages("site.yes")
          } else {
            messages("site.no")
          }
        ),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            changeLink
          ).withVisuallyHiddenText(messages("assessment.change.hidden", indexOfThisAssessment + 1))
        )
      )
    }
  }

}
