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
import pages.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService

final case class CategoryRecord(
  eori: String,
  recordId: String,
  finalComCode: String,
  category: Scenario,
  measurementUnit: Option[String],
  supplementaryUnit: Option[String],
  //The below stuff is just for audits
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
  ): EitherNec[ValidationError, CategoryRecord] =
    (
      getInitialCategoryInfo(userAnswers, recordId),
      userAnswers.getOptionalPageValueForOptionalBooleanPage(
        userAnswers,
        HasSupplementaryUnitPage(recordId),
        SupplementaryUnitPage(recordId)
      )
    ).parMapN { (initialCategorisationInfo, supplementaryUnit) =>
      val longerCategoryInfo      = userAnswers.get(LongerCategorisationDetailsQuery(recordId))
      val finalCategorisationInfo = longerCategoryInfo.getOrElse(initialCategorisationInfo)
      val longerCategoryAnswers   = longerCategoryInfo.map(_.getAnswersForQuestions(userAnswers, recordId))

      CategoryRecord(
        eori,
        recordId,
        finalCategorisationInfo.commodityCode,
        categorisationService.calculateResult(finalCategorisationInfo, userAnswers, recordId),
        finalCategorisationInfo.measurementUnit,
        supplementaryUnit,
        initialCategorisationInfo,
        initialCategorisationInfo.getAnswersForQuestions(userAnswers, recordId).count(_.isAnswered),
        userAnswers.get(HasSupplementaryUnitPage(recordId)).isDefined,
        longerCategoryInfo,
        longerCategoryAnswers.map(_.count(_.isAnswered)),
        longerCategoryAnswers.map(_.count(_.wasCopiedFromInitialAssessment))
      )
    }

  private def getInitialCategoryInfo(userAnswers: UserAnswers, recordId: String) =
    userAnswers
      .getPageValue(CategorisationDetailsQuery(recordId))
      .map(Right(_))
      .getOrElse(
        Left(NonEmptyChain.one(NoCategorisationDetailsForRecordId(CategorisationDetailsQuery(recordId), recordId)))
      )

}
