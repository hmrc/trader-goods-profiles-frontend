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

package pages

import models.{AssessmentAnswer, AssessmentAnswer2, UserAnswers}
import play.api.libs.json.JsPath
import queries.{CategorisationDetailsQuery2, LongerCategorisationDetailsQuery, RecordCategorisationsQuery}

import scala.util.{Failure, Success, Try}

case class ReassessmentPage(
  recordId: String,
  index: Int
) extends QuestionPage[AssessmentAnswer2] {
  override def path: JsPath = JsPath \ "reassessments" \ recordId \ index

  override def cleanup(
    value: Option[AssessmentAnswer2],
    updatedUserAnswers: UserAnswers,
    originalUserAnswers: UserAnswers
  ): Try[UserAnswers] =
    if (value.contains(AssessmentAnswer2.NoExemption)) {
      (for {
        categorisationInfo <- updatedUserAnswers.get(LongerCategorisationDetailsQuery(recordId))
        count               = categorisationInfo.categoryAssessmentsThatNeedAnswers.size
        //Go backwards to avoid recursion issues
        rangeToRemove       = ((index + 1) to count).reverse
      } yield rangeToRemove.foldLeft[Try[UserAnswers]](Success(updatedUserAnswers)) { (acc, currentIndexToRemove) =>
        acc.flatMap(_.remove(ReassessmentPage(recordId, currentIndexToRemove)))
      }).getOrElse(
        Failure(new InconsistentUserAnswersException(s"Could not find category assessment with index $index"))
      )
    } else {
      super.cleanup(value, updatedUserAnswers, originalUserAnswers)
    }
}