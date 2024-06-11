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
import queries.CategorisationQuery

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
      getCategory(answers),
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

  def getLastAssessmentPage(categorisationInfo: CategorisationInfo, category: Int): AssessmentPage =
    AssessmentPage(categorisationInfo.categoryAssessments.filter(_.category == category).last.id)

  def getCategory(answers: UserAnswers): EitherNec[ValidationError, Int] =
    answers.get(CategorisationQuery) match {
      case Some(categorisationInfo) =>
        val lastCategory1AssessmentId = getLastAssessmentPage(categorisationInfo, 1)
        answers.getPageValue(lastCategory1AssessmentId) match {
          case Right(AssessmentAnswer.NoExemption) => Right(1)
          case Right(_)                            =>
            val lastCategory2AssessmentId = getLastAssessmentPage(categorisationInfo, 2)
            answers.getPageValue(lastCategory2AssessmentId) match {
              case Right(AssessmentAnswer.NoExemption) => Right(2)
              case Right(_)                            => Right(3)
              case _                                   => Right(2)
            }
          case _                                   => Right(1)
        }
      case _                        => Left(NonEmptyChain.one(PageMissing(CategorisationQuery)))
    }

  //TODO: get measurementUnit from answers
  private def getMeasurementUnit(answers: UserAnswers): EitherNec[ValidationError, Option[String]] = Right(Some("1"))
}
