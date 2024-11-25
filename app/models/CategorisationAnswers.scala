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
import models.ott.CategorisationInfo
import org.apache.pekko.Done
import pages.categorisation.{AssessmentPage, HasSupplementaryUnitPage, ReassessmentPage, SupplementaryUnitPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import utils.Constants.firstAssessmentIndex

final case class CategorisationAnswers(
  assessmentValues: Seq[Option[AssessmentAnswer]],
  supplementaryUnit: Option[String]
)

object CategorisationAnswers {

  def build(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, CategorisationAnswers] =
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
  ): EitherNec[ValidationError, Seq[Option[AssessmentAnswer]]] =
    for {
      categorisationInfo       <- getCategorisationInfoForThisRecord(userAnswers, recordId)
      answeredQuestionsOptions <-
        getAssessmentsFromUserAnswers(categorisationInfo, userAnswers, recordId)
      answeredQuestionsOnly    <- getAnsweredQuestionsOnly(answeredQuestionsOptions, recordId)
      _                        <- ensureNoExemptionIsOnlyFinalAnswer(answeredQuestionsOnly, recordId)
      _                        <- ensureHaveAnsweredTheRightAmount(
                                    answeredQuestionsOnly,
                                    answeredQuestionsOptions.size,
                                    recordId
                                  )
      _                        <- ensureNoNotAnsweredYetInAnswers(answeredQuestionsOnly, recordId)
    } yield answeredQuestionsOptions.map(_.answer)

  private def getCategorisationInfoForThisRecord(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[NoCategorisationDetailsForRecordId, CategorisationInfo] = {
    val longerComCodeOpt = userAnswers
      .get(LongerCategorisationDetailsQuery(recordId))
    longerComCodeOpt match {
      case Some(longerComCode) => Right(longerComCode)
      case None                =>
        userAnswers
          .getPageValue(CategorisationDetailsQuery(recordId))
          .map(Right(_))
          .getOrElse(
            Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(CategorisationDetailsQuery(recordId), recordId)))
          )
    }
  }

  private def getAnsweredQuestionsOnly(
    answeredQuestionsOptions: Seq[AnsweredQuestions],
    recordId: String
  ) = {
    val answeredQuestionsOnly = answeredQuestionsOptions.filter(_.answer.isDefined)

    if (answeredQuestionsOnly.isEmpty && answeredQuestionsOptions.nonEmpty) {
      val errorPage = if (answeredQuestionsOptions.exists(x => x.reassessmentQuestion)) {
        ReassessmentPage(recordId, firstAssessmentIndex)
      } else {
        AssessmentPage(recordId, firstAssessmentIndex)
      }
      Left(NonEmptyChain.one(MissingAssessmentAnswers(errorPage)))
    } else {
      Right(answeredQuestionsOnly)
    }

  }

  private def getAssessmentsFromUserAnswers(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Seq[AnsweredQuestions]] = {
    val answers = categorisationInfo.getAnswersForQuestions(userAnswers, recordId)

    if (answers.isEmpty && categorisationInfo.categoryAssessmentsThatNeedAnswers.nonEmpty) {
      val errorPage = if (categorisationInfo.longerCode) {
        ReassessmentPage(recordId, firstAssessmentIndex)
      } else {
        AssessmentPage(recordId, firstAssessmentIndex)
      }

      Left(NonEmptyChain(PageMissing(errorPage)))
    } else {
      Right(answers)
    }
  }

  private def ensureNoExemptionIsOnlyFinalAnswer(
    answeredQuestionsOnly: Seq[AnsweredQuestions],
    recordId: String
  ): EitherNec[ValidationError, Done]             =
    if (answeredQuestionsOnly.isEmpty) {
      Right(Done)
    } else {

      //Last answer can be a NoExemption. Others can't
      val allExceptLastAnswer          = answeredQuestionsOnly.reverse.tail
      val noExemptionsBeforeLastAnswer =
        allExceptLastAnswer.filter(ass => ass.answer.contains(AssessmentAnswer.NoExemption))

      if (noExemptionsBeforeLastAnswer.isEmpty) {
        Right(Done)
      } else {
        val errors = noExemptionsBeforeLastAnswer.map { ass =>
          val errorPage = if (ass.reassessmentQuestion) {
            ReassessmentPage(recordId, ass.index)
          } else {
            AssessmentPage(recordId, ass.index)
          }
          UnexpectedNoExemption(errorPage)
        }

        val defaultErrorPage = if (answeredQuestionsOnly.exists(_.reassessmentQuestion)) {
          LongerCategorisationDetailsQuery(recordId)
        } else {
          CategorisationDetailsQuery(recordId)
        }
        val nec              =
          NonEmptyChain
            .fromSeq(errors)
            .getOrElse(NonEmptyChain.one(UnexpectedNoExemption(defaultErrorPage)))
        Left(nec)
      }
    }

  private def ensureNoNotAnsweredYetInAnswers(
    answeredQuestions: Seq[AnsweredQuestions],
    recordId: String
  ): EitherNec[ValidationError, Done] = {
    val notAnsweredYetAssessments =
      answeredQuestions.filter(ass => ass.answer.contains(AssessmentAnswer.NotAnsweredYet))

    if (notAnsweredYetAssessments.isEmpty) {
      Right(Done)
    } else {
      val errors = notAnsweredYetAssessments.map(ass => MissingAssessmentAnswers(ReassessmentPage(recordId, ass.index)))
      val nec    =
        NonEmptyChain
          .fromSeq(errors)
          .getOrElse(NonEmptyChain.one(MissingAssessmentAnswers(LongerCategorisationDetailsQuery(recordId))))
      Left(nec)
    }
  }
  private def ensureHaveAnsweredTheRightAmount(
    answeredQuestionsOnly: Seq[AnsweredQuestions],
    totalQuestions: Int,
    recordId: String
  ): Either[NonEmptyChain[ValidationError], Done] =
    if (answeredQuestionsOnly.isEmpty) {
      Right(Done)
    } else {
      val lastAnswerIsExemption = answeredQuestionsOnly.last.answer.contains(AssessmentAnswer.NoExemption)

      if (lastAnswerIsExemption || totalQuestions == answeredQuestionsOnly.size) {
        Right(Done)
      } else {
        val errorPage = if (answeredQuestionsOnly.exists(_.reassessmentQuestion)) {
          LongerCategorisationDetailsQuery(recordId)
        } else {
          CategorisationDetailsQuery(recordId)
        }
        Left(NonEmptyChain.one(MissingAssessmentAnswers(errorPage)))
      }

    }
}
