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
import models.ott.Exemption
import models.{CheckMode, UserAnswers}
import pages.AssessmentPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AssessmentsSummary {

  //TODO is this tested via CYA? Make sure case where can't find exemption is tested
  //TODO this is a pain to use
  def row(
    answers: UserAnswers,
    assessmentId: String,
    numberOfThisAssessment: Int,
    numberOfAssessments: Int,
    exemptions: Seq[Exemption]
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AssessmentPage(assessmentId)).map { answer =>
      val value = answer.toString
      val descriptiveText = if (value == "none") {
        "assessment.exemption.none.checkYourAnswersLabel"
      } else {
        //TODO if it can't be found???
        val exemption = exemptions.find(x => x.code == value).get
        messages("assessment.exemption", exemption.code, exemption.description)
      }

      SummaryListRowViewModel(
        key = messages("assessment.checkYourAnswersLabel", numberOfThisAssessment, numberOfAssessments),
        value = ValueViewModel(descriptiveText),
        actions = Seq(
          ActionItemViewModel("site.change", routes.AssessmentController.onPageLoad(CheckMode, assessmentId).url)
            .withVisuallyHiddenText(messages("assessment.change.hidden"))
        )
      )
    }
}
