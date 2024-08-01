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
import pages.{HasSupplementaryUnitUpdatePage, SupplementaryUnitUpdatePage}
import play.api.libs.json.{Json, OFormat}
import queries.MeasurementQuery

final case class SupplementaryRequest(
  eori: String,
  recordId: String,
  hasSupplementaryUnit: Option[Boolean] = None,
  supplementaryUnit: Option[String] = None,
  measurementUnit: Option[String] = None
)

object SupplementaryRequest {

  implicit lazy val format: OFormat[SupplementaryRequest] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, SupplementaryRequest] = {
    val hasSupplementaryUnitEither = getHasSupplementaryUnit(answers, recordId)
    val supplementaryUnitEither    = getSupplementaryUnit(answers, recordId)
    val measurementUnitEither      = getMeasurementUnit(answers, recordId)

    hasSupplementaryUnitEither
      .flatMap { hasSupplementaryUnit =>
        if (hasSupplementaryUnit) {
          (supplementaryUnitEither, measurementUnitEither).parMapN { (supplementaryUnit, measurementUnit) =>
            SupplementaryRequest(
              eori = eori,
              recordId = recordId,
              hasSupplementaryUnit = Some(true),
              supplementaryUnit = Some(supplementaryUnit),
              measurementUnit = Some(measurementUnit)
            )
          }
        } else {
          Right(
            SupplementaryRequest(
              eori = eori,
              recordId = recordId,
              hasSupplementaryUnit = Some(false),
              supplementaryUnit = None,
              measurementUnit = None
            )
          )
        }
      }
      .orElse {
        (supplementaryUnitEither, measurementUnitEither).parMapN { (supplementaryUnit, measurementUnit) =>
          SupplementaryRequest(
            eori = eori,
            recordId = recordId,
            hasSupplementaryUnit = None,
            supplementaryUnit = Some(supplementaryUnit),
            measurementUnit = Some(measurementUnit)
          )
        }
      }
  }

  private def getHasSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Boolean] =
    userAnswers.getPageValue(HasSupplementaryUnitUpdatePage(recordId))

  private def getSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, String] =
    userAnswers.getPageValue(SupplementaryUnitUpdatePage(recordId))

  private def getMeasurementUnit(answers: UserAnswers, recordId: String): EitherNec[ValidationError, String] = {
    val query = MeasurementQuery(recordId)
    answers
      .get(query)
      .toRight(NonEmptyChain.one(if (answers.isDefined(query)) RecordIdMissing(query) else PageMissing(query)))
  }

}
