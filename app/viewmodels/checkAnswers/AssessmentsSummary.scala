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
import models.{AssessmentAnswer2, CheckMode, UserAnswers}
import pages.{AssessmentPage, AssessmentPage2}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.AssessmentCyaKey
object AssessmentsSummary {

  def row2(
    recordId: String,
    answers: UserAnswers,
    assessment: CategoryAssessment,
    indexOfThisAssessment: Int
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AssessmentPage2(recordId, indexOfThisAssessment)).map{ answer =>
      val codes = assessment.exemptions.map(_.code)
      val descriptions = assessment.exemptions.map(_.description)

      SummaryListRowViewModel(
        key = KeyViewModel(AssessmentCyaKey(codes, descriptions, (indexOfThisAssessment + 1).toString).content)
          .withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(
          if (answer == AssessmentAnswer2.Exemption) {
            messages("site.yes")
          } else {
            messages("site.no")
          }
        ),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            routes.AssessmentController.onPageLoad2(CheckMode, recordId, indexOfThisAssessment).url
          ).withVisuallyHiddenText(messages("assessment.change.hidden", indexOfThisAssessment + 1))
        )
      )
    }

  def row(
    recordId: String,
    answers: UserAnswers,
    assessment: CategoryAssessment,
    indexOfThisAssessment: Int
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AssessmentPage(recordId, indexOfThisAssessment)).map { answer =>
      val codes        = assessment.exemptions.map(_.code)
      val descriptions = assessment.exemptions.map(_.description)

      SummaryListRowViewModel(
        key = KeyViewModel(AssessmentCyaKey(codes, descriptions, (indexOfThisAssessment + 1).toString).content)
          .withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(
          if (answer.toString == "true") {
            messages("site.yes")
          } else {
            messages("site.no")
          }
        ),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            routes.AssessmentController.onPageLoad(CheckMode, recordId, indexOfThisAssessment).url
          ).withVisuallyHiddenText(messages("assessment.change.hidden", indexOfThisAssessment + 1))
        )
      )
    }
}
