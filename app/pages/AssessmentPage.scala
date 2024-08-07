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

import models.{AssessmentAnswer, UserAnswers}
import play.api.libs.json.JsPath
import queries.RecordCategorisationsQuery

import scala.util.{Failure, Success, Try}

case class AssessmentPage(
  recordId: String,
  index: Int,
  shouldRedirectToCya: Boolean = false,
  cleanupAll: Boolean = false
) extends QuestionPage[AssessmentAnswer] {

  override def path: JsPath = JsPath \ "assessments" \ recordId \ index

  override def cleanup(
    value: Option[AssessmentAnswer],
    updatedUserAnswers: UserAnswers,
    originalUserAnswers: UserAnswers
  ): Try[UserAnswers] =
    if ((value.contains(AssessmentAnswer.NoExemption) && !shouldRedirectToCya) || cleanupAll) {
      (for {
        recordQuery        <- updatedUserAnswers.get(RecordCategorisationsQuery)
        categorisationInfo <- recordQuery.records.get(recordId)
        count               = categorisationInfo.categoryAssessments.size
        //Go backwards to avoid recursion issues
        rangeToRemove       = if (cleanupAll) (index to count).reverse else ((index + 1) to count).reverse
      } yield rangeToRemove.foldLeft[Try[UserAnswers]](Success(updatedUserAnswers)) { (acc, currentIndexToRemove) =>
        acc.flatMap(_.remove(AssessmentPage(recordId, currentIndexToRemove)))
      }).getOrElse(
        Failure(new InconsistentUserAnswersException(s"Could not find category assessment with index $index"))
      )
    } else {
      super.cleanup(value, updatedUserAnswers, originalUserAnswers)
    }
}
