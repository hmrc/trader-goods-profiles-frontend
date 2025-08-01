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
import pages.categorisation.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService

final case class CategoryRecord(
  eori: String,
  recordId: String,
  finalComCode: String,
  category: Scenario,
  measurementUnit: Option[String],
  supplementaryUnit: Option[String],
  initialCategoryInfo: CategorisationInfo,
  assessmentsAnswered: Int,
  wasSupplementaryUnitAsked: Boolean,
  longerCategoryInfo: Option[CategorisationInfo] = None,
  longerAssessmentsAnswered: Option[Int] = None,
  answersCopiedOverFromShortToLong: Option[Int] = None
)

object CategoryRecord {

  def build(
    userAnswers: UserAnswers,
    eori: String,
    recordId: String,
    categorisationService: CategorisationService
  ): EitherNec[ValidationError, CategoryRecord] = {

    val initialCategoryInfoEither = getInitialCategoryInfo(userAnswers, recordId)

    val supplementaryUnitOpt = userAnswers.getOptionalPageValueForOptionalBooleanPage(
      userAnswers,
      HasSupplementaryUnitPage(recordId),
      SupplementaryUnitPage(recordId)
    )

    val longerCategoryInfoOpt = userAnswers.get(LongerCategorisationDetailsQuery(recordId))

    (initialCategoryInfoEither, supplementaryUnitOpt).parMapN { (initialCategorisationInfo, supplementaryUnit) =>
      val finalCategorisationInfo = longerCategoryInfoOpt.getOrElse(initialCategorisationInfo)
      val category                = categorisationService.calculateResult(finalCategorisationInfo, userAnswers, recordId)

      CategoryRecord(
        eori,
        recordId,
        finalCategorisationInfo.commodityCode,
        category,
        finalCategorisationInfo.measurementUnit,
        supplementaryUnit,
        initialCategorisationInfo,
        initialCategorisationInfo.getAnswersForQuestions(userAnswers, recordId).count(_.isAnswered),
        userAnswers.get(HasSupplementaryUnitPage(recordId)).isDefined,
        longerCategoryInfoOpt,
        longerCategoryInfoOpt.map(_.getAnswersForQuestions(userAnswers, recordId).count(_.isAnswered)),
        longerCategoryInfoOpt.map(
          _.getAnswersForQuestions(userAnswers, recordId).count(_.wasCopiedFromInitialAssessment)
        )
      )
    }
  }

  private def getInitialCategoryInfo(userAnswers: UserAnswers, recordId: String) = {
    val result = userAnswers.getPageValue(CategorisationDetailsQuery(recordId))
    if (result.isLeft) {}
    result
      .map(Right(_))
      .getOrElse(
        Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(CategorisationDetailsQuery(recordId), recordId)))
      )
  }
}
