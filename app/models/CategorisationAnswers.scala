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

package models

import cats.data.{EitherNec, NonEmptyChain}
import cats.implicits.catsSyntaxTuple2Parallel
import models.AssessmentAnswer.NoExemption
import models.ott.{CategorisationInfo, CategoryAssessment}
import org.apache.pekko.Done
import pages.{AssessmentPage, HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}
import queries.RecordCategorisationsQuery

final case class CategorisationAnswers(
  assessmentValues: Seq[AssessmentAnswer],
  supplementaryUnit: Option[Int]
)

object CategorisationAnswers {

  implicit lazy val format: OFormat[CategorisationAnswers] = Json.format

  private case class CategorisationDetails(index: Int, assessment: CategoryAssessment, answer: AssessmentAnswer)

  def build(userAnswers: UserAnswers, recordId: String): EitherNec[ValidationError, CategorisationAnswers] =
    (
      buildAssessmentDetails(userAnswers, recordId),
      getSupplementaryUnit(userAnswers, recordId)
    ).parMapN(CategorisationAnswers.apply)

  private def getSupplementaryUnit(userAnswers: UserAnswers, recordId: String) =
    userAnswers.getOptionalPageValueForOptionalBooleanPage(
      userAnswers,
      HasSupplementaryUnitPage(recordId),
      SupplementaryUnitPage(recordId)
    )

  private def buildAssessmentDetails(
    userAnswers: UserAnswers,
    recordId: String
  ): Either[NonEmptyChain[ValidationError], Seq[AssessmentAnswer]] =
    for {
      recordCategorisations <- userAnswers.getPageValue(RecordCategorisationsQuery)
      categorisationInfo    <- getCategorisationInfoForThisRecord(recordCategorisations, recordId)
      answeredAssessments   <- getAssessmentsFromUserAnswers(categorisationInfo, userAnswers, recordId)
      _                     <- ensureNoExemptionIsOnlyFinalAnswer(answeredAssessments, recordId)
      _                     <- ensureHaveAnsweredTheRightAmount(answeredAssessments, categorisationInfo.categoryAssessments.size)
    //  _                     <- ensureHaveNotSkippedAny(answeredAssessments)
      justTheAnswers         = answeredAssessments.map(_.answer)
    } yield justTheAnswers

  private def getCategorisationInfoForThisRecord(recordCategorisations: RecordCategorisations, recordId: String) =
    recordCategorisations.records
      .get(recordId)
      .map(Right(_))
      .getOrElse(Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(RecordCategorisationsQuery, recordId))))

  private def getAssessmentsFromUserAnswers(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Seq[CategorisationDetails]] = {
    val answers = categorisationInfo.categoryAssessments.zipWithIndex
      .map(assessment =>
        (assessment._2, assessment._1, userAnswers.get(AssessmentPage(recordId, assessment._2)))
      )
      .filter(x => x._3.isDefined)
      .map(x => CategorisationDetails(x._1, x._2, x._3.get))

    if (answers.isEmpty) {
      Left(NonEmptyChain(PageMissing(AssessmentPage(recordId, 1))))
    } else {
      Right(answers)
    }
  }

  private def ensureNoExemptionIsOnlyFinalAnswer(
    answeredAssessments: Seq[CategorisationDetails],
    recordId: String
  ): EitherNec[ValidationError, Done] = {

    //Last answer can be a NoExemption. Others can't
    val allExceptLastAnswer          = answeredAssessments.reverse.tail
    val noExemptionsBeforeLastAnswer = allExceptLastAnswer.filter(ass => ass.answer == NoExemption)

    if (noExemptionsBeforeLastAnswer.isEmpty) {
      Right(Done)
    } else {
      val errors = noExemptionsBeforeLastAnswer.map(ass => UnexpectedNoExemption(AssessmentPage(recordId, ass.index)))
      val nec    =
        NonEmptyChain.fromSeq(errors).getOrElse(NonEmptyChain.one(UnexpectedNoExemption(RecordCategorisationsQuery)))
      Left(nec)
    }

  }

  private def ensureHaveAnsweredTheRightAmount(
    answeredAssessments: Seq[CategorisationDetails],
    assessmentCount: Int
  ): Either[NonEmptyChain[ValidationError], Done] = {

    val lastAnswerIsExemption = answeredAssessments.last.answer.equals(NoExemption)
    val amountAnswered        = answeredAssessments.size

    if (lastAnswerIsExemption || amountAnswered == assessmentCount) {
      Right(Done)
    } else {
      Left(NonEmptyChain.one(MissingAssessmentAnswers(RecordCategorisationsQuery)))
    }

  }

}
