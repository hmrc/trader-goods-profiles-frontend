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
import models.AssessmentAnswer.{NoExemption, NotAnsweredYet}
import models.ott.{CategorisationInfo, CategorisationInfo2, CategoryAssessment}
import org.apache.pekko.Done
import pages.{AssessmentPage, AssessmentPage2, HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}
import queries.{CategorisationDetailsQuery, CategorisationDetailsQuery2}
import utils.Constants.firstAssessmentIndex
import queries.RecordCategorisationsQuery

final case class CategorisationAnswers(
  assessmentValues: Seq[AssessmentAnswer],
  supplementaryUnit: Option[String]
)

final case class CategorisationAnswers2(
  assessmentValues: Seq[Option[AssessmentAnswer2]]
  //supplementaryUnit: Option[String]
)

object CategorisationAnswers2 {

  // implicit lazy val format: OFormat[CategorisationAnswers2] = Json.format

  def build(userAnswers: UserAnswers, recordId: String): EitherNec[ValidationError, CategorisationAnswers2] =
    buildAssessmentDetails(userAnswers, recordId)
      .map(CategorisationAnswers2(_))

  private def buildAssessmentDetails(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Seq[Option[AssessmentAnswer2]]] =
    for {
      categorisationInfo       <- getCategorisationInfoForThisRecord(userAnswers, recordId)
      answeredQuestionsOptions <- getAssessmentsFromUserAnswers(categorisationInfo, userAnswers, recordId)
      answeredQuestionsOnly     = answeredQuestionsOptions.filter(_.answer.isDefined)
      _                        <- ensureNoExemptionIsOnlyFinalAnswer(answeredQuestionsOnly, recordId)
      _                        <- ensureHaveAnsweredTheRightAmount(
                                    answeredQuestionsOnly,
                                    answeredQuestionsOptions.size,
                                    recordId
                                  )
    } yield answeredQuestionsOptions.map(_.answer)

  private def getCategorisationInfoForThisRecord(userAnswers: UserAnswers, recordId: String) =
    userAnswers
      .getPageValue(CategorisationDetailsQuery2(recordId))
      .map(Right(_))
      .getOrElse(
        Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(CategorisationDetailsQuery2(recordId), recordId)))
      )

  private def getAssessmentsFromUserAnswers(
    categorisationInfo: CategorisationInfo2,
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Seq[AnsweredQuestions]] = {
    val answers = categorisationInfo.getAnswersForQuestions(userAnswers, recordId)

    if (answers.isEmpty) {
      Left(NonEmptyChain(PageMissing(AssessmentPage2(recordId, firstAssessmentIndex))))
    } else {
      Right(answers)
    }
  }

  private def ensureNoExemptionIsOnlyFinalAnswer(
    answeredQuestionsOnly: Seq[AnsweredQuestions],
    recordId: String
  ): EitherNec[ValidationError, Done] = {

    //Last answer can be a NoExemption. Others can't
    val allExceptLastAnswer          = answeredQuestionsOnly.reverse.tail
    val noExemptionsBeforeLastAnswer =
      allExceptLastAnswer.filter(ass => ass.answer.contains(AssessmentAnswer2.NoExemption))

    if (noExemptionsBeforeLastAnswer.isEmpty) {
      Right(Done)
    } else {
      val errors = noExemptionsBeforeLastAnswer.map(ass => UnexpectedNoExemption(AssessmentPage2(recordId, ass.index)))
      val nec    =
        NonEmptyChain
          .fromSeq(errors)
          .getOrElse(NonEmptyChain.one(UnexpectedNoExemption(CategorisationDetailsQuery2(recordId))))
      Left(nec)
    }
  }

  private def ensureHaveAnsweredTheRightAmount(
    answeredQuestionsOnly: Seq[AnsweredQuestions],
    totalQuestions: Int,
    recordId: String
  ): Either[NonEmptyChain[ValidationError], Done] = {

    val lastAnswerIsExemption = answeredQuestionsOnly.last.answer.contains(AssessmentAnswer2.NoExemption)

    if (lastAnswerIsExemption || totalQuestions == answeredQuestionsOnly.size) {
      Right(Done)
    } else {
      Left(NonEmptyChain.one(MissingAssessmentAnswers(CategorisationDetailsQuery2(recordId))))
    }

  }
}

object CategorisationAnswers {

  implicit lazy val format: OFormat[CategorisationAnswers] = Json.format

  private case class CategorisationDetails(index: Int, assessment: CategoryAssessment, answer: AssessmentAnswer)
  private case class CategorisationDetailsOption(
    index: Int,
    assessment: CategoryAssessment,
    answer: Option[AssessmentAnswer]
  )

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
      _                     <- ensureNoNotAnsweredYetInAnswers(answeredAssessments, recordId)
      _                     <- ensureHaveAnsweredTheRightAmount(answeredAssessments, countAssessmentsThatRequireAnswers(categorisationInfo))
      justTheAnswers         = answeredAssessments.map(_.answer)
    } yield justTheAnswers

  private def countAssessmentsThatRequireAnswers(categorisationInfo: CategorisationInfo): Int =
    categorisationInfo.categoryAssessments.takeWhile(a => !(a.category == 2 && a.exemptions.isEmpty)).size

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
        CategorisationDetailsOption(
          assessment._2,
          assessment._1,
          userAnswers.get(AssessmentPage(recordId, assessment._2))
        )
      )
      .filter(x => x.answer.isDefined)
      .map(x => CategorisationDetails(x.index, x.assessment, x.answer.get))

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

  private def ensureNoNotAnsweredYetInAnswers(
    answeredAssessments: Seq[CategorisationDetails],
    recordId: String
  ): EitherNec[ValidationError, Done] = {
    val notAnsweredYetAssessments = answeredAssessments.filter(ass => ass.answer == NotAnsweredYet)

    if (notAnsweredYetAssessments.isEmpty) {
      Right(Done)
    } else {
      val errors = notAnsweredYetAssessments.map(ass => MissingAssessmentAnswers(AssessmentPage(recordId, ass.index)))
      val nec    =
        NonEmptyChain.fromSeq(errors).getOrElse(NonEmptyChain.one(MissingAssessmentAnswers(RecordCategorisationsQuery)))
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
