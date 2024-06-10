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

import cats.data.EitherNec
import cats.implicits.catsSyntaxTuple3Parallel
import pages.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}

final case class CategoryRecord(
  eori: String,
  recordId: String,
  category: Option[Int] = None,
  supplementaryUnit: Option[Int] = None,
  measurementUnit: Option[String] = None
)

object CategoryRecord {

  implicit lazy val format: OFormat[CategoryRecord] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, CategoryRecord] =
    (
      getCategory(answers),
      answers.getOptionalPageIntValue(answers, HasSupplementaryUnitPage, SupplementaryUnitPage),
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

  //TODO: get category from answers
  private def getCategory(answers: UserAnswers): EitherNec[ValidationError, Option[Int]] = Right(Some(1))

  //TODO: get measurementUnit from answers
  private def getMeasurementUnit(answers: UserAnswers): EitherNec[ValidationError, Option[String]] = Right(Some("1"))

}
