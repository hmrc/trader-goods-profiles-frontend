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
import pages.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService

final case class CategoryRecord(
  eori: String,
  recordId: String,
  comcode: String,
  category: Scenario,
  categoryAssessmentsWithExemptions: Int,
  measurementUnit: Option[String],
  supplementaryUnit: Option[String] = None
)

object CategoryRecord {

  def build(
    userAnswers: UserAnswers,
    eori: String,
    recordId: String,
    categorisationService: CategorisationService
  ): EitherNec[ValidationError, CategoryRecord] =
    (
      getCategorisationInfoForThisRecord(userAnswers, recordId),
      userAnswers.getOptionalPageValueForOptionalBooleanPage(
        userAnswers,
        HasSupplementaryUnitPage(recordId),
        SupplementaryUnitPage(recordId)
      )
    ).parMapN((categorisationInfo, supplementaryUnit) =>
      CategoryRecord(
        eori,
        recordId,
        categorisationInfo.commodityCode,
        categorisationService.calculateResult(categorisationInfo, userAnswers, recordId),
        categorisationInfo.getAnswersForQuestions(userAnswers, recordId).count(x => x.answer.isDefined),
        categorisationInfo.measurementUnit,
        supplementaryUnit
      )
    )

  private def getCategorisationInfoForThisRecord(userAnswers: UserAnswers, recordId: String) =
    userAnswers.get(LongerCategorisationDetailsQuery(recordId)) match {
      case Some(catInfo) => Right(catInfo)
      case _             =>
        userAnswers
          .getPageValue(CategorisationDetailsQuery(recordId))
          .map(Right(_))
          .getOrElse(
            Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(CategorisationDetailsQuery(recordId), recordId)))
          )
    }

}
