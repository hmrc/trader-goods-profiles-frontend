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
import cats.implicits.catsSyntaxTuple3Parallel
import models.ott.CategorisationInfo
import pages.{AssessmentPage, HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}
import queries.RecordCategorisationsQuery

final case class CategoryRecord(
  eori: String,
  recordId: String,
  category: Int,
  supplementaryUnit: Option[Int] = None,
  measurementUnit: Option[String] = None
)

object CategoryRecord {

  implicit lazy val format: OFormat[CategoryRecord] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, CategoryRecord] =
    (
      getCategory(answers, recordId),
      answers.getOptionalPageValue(answers, HasSupplementaryUnitPage, SupplementaryUnitPage),
      getMeasurementUnit(answers)
    ).parMapN((category, supplementaryUnit, measurementUnit) =>
      CategoryRecord(
        eori = eori,
        recordId = recordId,
        category = category,
        supplementaryUnit = supplementaryUnit,
        measurementUnit = measurementUnit
      )
    )

  def getCategory(answers: UserAnswers, recordId: String): EitherNec[ValidationError, Int] =
    answers.get(RecordCategorisationsQuery) match {
      case Some(recordCategorisations) =>
        recordCategorisations.records.get(recordId) match {
          case Some(categorisationInfo) => chooseCategory(recordId, answers, categorisationInfo)
          case _                        => Left(NonEmptyChain.one(RecordIdMissing(RecordCategorisationsQuery)))
        }
      case _                           => Left(NonEmptyChain.one(PageMissing(RecordCategorisationsQuery)))
    }

  def chooseCategory2Or3(
    recordId: String,
    answers: UserAnswers,
    categorisationInfo: CategorisationInfo
  ): EitherNec[ValidationError, Int] = {
    val category2AssessmentsCount = categorisationInfo.categoryAssessments.count(_.category == 2)
    if (category2AssessmentsCount > 0) {
      answers.getPageValue(AssessmentPage(recordId, categorisationInfo.categoryAssessments.length - 1)) match {
        case Right(AssessmentAnswer.NoExemption) => Right(2)
        case Right(_)                            => Right(3)
        case _                                   => Right(2)
      }
    } else {
      Left(NonEmptyChain.one(NoCategory2Assessments(RecordCategorisationsQuery)))
    }
  }

  def chooseCategory(
    recordId: String,
    answers: UserAnswers,
    categorisationInfo: CategorisationInfo
  ): EitherNec[ValidationError, Int] = {
    val category1AssessmentsCount = categorisationInfo.categoryAssessments.count(_.category == 1)
    if (category1AssessmentsCount > 0) {
      answers.getPageValue(
        AssessmentPage(recordId, category1AssessmentsCount - 1)
      ) match {
        case Right(AssessmentAnswer.NoExemption) => Right(1)
        case Right(_)                            => chooseCategory2Or3(recordId, answers, categorisationInfo)
        case _                                   => Right(1)
      }
    } else {
      Left(NonEmptyChain.one(NoCategory1Assessments(RecordCategorisationsQuery)))
    }
  }

  //TODO: get measurementUnit from answers
  private def getMeasurementUnit(answers: UserAnswers): EitherNec[ValidationError, Option[String]] = Right(Some("1"))
}
