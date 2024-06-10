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
import queries.CategorisationQuery

final case class CategorisationAnswers(
  assessmentValues: Seq[AssessmentAnswer],
  supplementaryUnit: Option[Int]
)

object CategorisationAnswers {

  implicit lazy val format: OFormat[CategorisationAnswers] = Json.format

  def build(userAnswers: UserAnswers): EitherNec[ValidationError, CategorisationAnswers] =
    (
      getAssessmentAnswers(userAnswers),
      userAnswers.getOptionalPageValueForOptionalBooleanPage(
        userAnswers,
        HasSupplementaryUnitPage,
        SupplementaryUnitPage
      )
    ).parMapN(CategorisationAnswers.apply)

  private def getAssessmentAnswers(userAnswers: UserAnswers) =
    for {
      categorisationInfo  <- userAnswers.getPageValue(CategorisationQuery)
      answeredAssessments <- getAnsweredAssessments(categorisationInfo, userAnswers)
      _                   <- ensureNoExemptionIsOnlyFinalAnswer(answeredAssessments)
      _                   <- ensureHaveAnsweredTheRightAmount(answeredAssessments, categorisationInfo.categoryAssessments.size)
      justTheAnswers       = answeredAssessments.map(_._2)
    } yield justTheAnswers

  private def getAnsweredAssessments(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers
  ): EitherNec[ValidationError, Seq[(CategoryAssessment, AssessmentAnswer)]] = {
    val answers = categorisationInfo.categoryAssessments
      .map(assessment => (assessment, userAnswers.get(AssessmentPage(assessment.id))))
      .takeWhile(x => x._2.isDefined)
      .map(x => (x._1, x._2.get))

    if (answers.isEmpty) {
      Left(NonEmptyChain(PageMissing(AssessmentPage(categorisationInfo.categoryAssessments.head.id))))
    } else {
      Right(answers)
    }
  }

  private def ensureNoExemptionIsOnlyFinalAnswer(
    answeredAssessments: Seq[(CategoryAssessment, AssessmentAnswer)]
  ): EitherNec[ValidationError, Done] = {

    //Last answer can be a NoExemption. Others can't
    val allExceptLastAnswer          = answeredAssessments.reverse.tail
    val noExemptionsBeforeLastAnswer = allExceptLastAnswer.filter(ass => ass._2 == NoExemption)

    if (noExemptionsBeforeLastAnswer.isEmpty) {
      Right(Done)
    } else {
      val errors = noExemptionsBeforeLastAnswer.map(ass => UnexpectedNoExemption(AssessmentPage(ass._1.id)))
      val nec    = NonEmptyChain.fromSeq(errors).getOrElse(NonEmptyChain.one(UnexpectedNoExemption(CategorisationQuery)))
      Left(nec)
    }

  }

  private def ensureHaveAnsweredTheRightAmount(
    answeredAssessments: Seq[(CategoryAssessment, AssessmentAnswer)],
    assessmentCount: Int
  ): Either[NonEmptyChain[ValidationError], Done] = {

    val lastAnswerIsExemption = answeredAssessments.last._2.equals(NoExemption)
    val amountAnswered        = answeredAssessments.size

    if (lastAnswerIsExemption || amountAnswered == assessmentCount) {
      Right(Done)
    } else {
      Left(NonEmptyChain.one(MissingAssessmentAnswers(CategorisationQuery)))
    }

  }

}
