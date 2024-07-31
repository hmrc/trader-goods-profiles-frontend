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
import pages.{HasSupplementaryUnitUpdatePage, SupplementaryUnitUpdatePage}
import play.api.libs.json.{Json, OFormat}
import queries.MeasurementQuery

final case class SupplementaryRequest(
  eori: String,
  recordId: String,
  hasSupplementaryUnit: Option[Boolean],
  supplementaryUnit: Option[String],
  measurementUnit: Option[String]
)

object SupplementaryRequest {

  implicit lazy val format: OFormat[SupplementaryRequest] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, SupplementaryRequest] =
    (
      getHasSupplementaryUnit(answers, recordId),
      getSupplementaryUnit(answers, recordId),
      getMeasurementUnit(answers, recordId)
    ).parMapN((hasSupplementaryUnit, supplementaryUnit, measurementUnit) =>
      SupplementaryRequest(
        eori = eori,
        hasSupplementaryUnit = hasSupplementaryUnit,
        recordId = recordId,
        supplementaryUnit = supplementaryUnit,
        measurementUnit = measurementUnit
      )
    )
  private def getHasSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Option[Boolean]]                                                                    =
    Right(userAnswers.getPageValue(HasSupplementaryUnitUpdatePage(recordId)).toOption)

  private def getSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Option[String]] =
    Right(userAnswers.getPageValue(SupplementaryUnitUpdatePage(recordId)).toOption)

  private def getMeasurementUnit(answers: UserAnswers, recordId: String): EitherNec[ValidationError, Option[String]] =
    Right(answers.getPageValue(MeasurementQuery).map(_.getOrElse(recordId, "")).toOption)

}
