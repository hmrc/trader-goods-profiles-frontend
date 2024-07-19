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
import models.AssessmentAnswer.{NoExemption, NotAnsweredYet}
import models.ott.CategorisationInfo
import pages.{AssessmentPage, HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}

final case class CategoryRecord(
  eori: String,
  recordId: String,
  comcode: Option[String] = None,
  category: Int,
  categoryAssessmentsWithExemptions: Int,
  supplementaryUnit: Option[String] = None,
  measurementUnit: Option[String] = None
)

object CategoryRecord {

  implicit lazy val format: OFormat[CategoryRecord] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, CategoryRecord] =
    (
      getCategory(answers, recordId),
      answers.getOptionalPageValueForOptionalBooleanPage(
        answers,
        HasSupplementaryUnitPage(recordId),
        SupplementaryUnitPage(recordId)
      ),
      getMeasurementUnit(answers, recordId)
    ).parMapN((categoryDetails, supplementaryUnit, measurementUnit) =>
      CategoryRecord(
        eori = eori,
        recordId = recordId,
        comcode = getLongerCommodityCode(answers, recordId),
        category = categoryDetails.category,
        categoryAssessmentsWithExemptions = categoryDetails.categoryAssessmentsWithExemptions,
        supplementaryUnit = supplementaryUnit,
        measurementUnit = measurementUnit
      )
    )

  private def getLongerCommodityCode(answers: UserAnswers, recordId: String): Option[String] =
    answers.get(LongerCommodityQuery(recordId)).map(_.commodityCode)

  private val CATEGORY_1 = 1
  private val CATEGORY_2 = 2
  private val STANDARD   = 3

  private case class GetCategoryReturn(category: Int, categoryAssessmentsWithExemptions: Int)

  private def getCategory(answers: UserAnswers, recordId: String): EitherNec[ValidationError, GetCategoryReturn] =
    answers
      .get(RecordCategorisationsQuery)
      .map { recordCategorisations =>
        recordCategorisations.records
          .get(recordId)
          .map { categorisationInfo =>
            val exemptionsCount = getHowManyAssessmentsHadExemptions(recordId, answers, categorisationInfo)
            val category        = chooseCategory(recordId, answers, categorisationInfo)

            Right(GetCategoryReturn(category, exemptionsCount))
          }
          .getOrElse(Left(NonEmptyChain.one(RecordIdMissing(RecordCategorisationsQuery))))
      }
      .getOrElse(Left(NonEmptyChain.one(PageMissing(RecordCategorisationsQuery))))

  private def chooseCategory2Or3(
    recordId: String,
    answers: UserAnswers,
    categorisationInfo: CategorisationInfo
  ): Int = {
    val category2AssessmentsCount = categorisationInfo.categoryAssessments.count(_.category == 2)
    if (category2AssessmentsCount > 0) {
      answers.getPageValue(AssessmentPage(recordId, categorisationInfo.categoryAssessments.length - 1)) match {
        case Right(AssessmentAnswer.NoExemption) => CATEGORY_2
        case Right(_)                            => STANDARD
        case _                                   => CATEGORY_2
      }
    } else {
      val category1AssessmentsCount = categorisationInfo.categoryAssessments.count(_.category == 1)
      if (category1AssessmentsCount != 0 && category2AssessmentsCount == 0) {
        CATEGORY_2
      } else {
        STANDARD
      }
    }
  }

  private def chooseCategory(
    recordId: String,
    answers: UserAnswers,
    categorisationInfo: CategorisationInfo
  ): Int = {
    val category1AssessmentsCount = categorisationInfo.categoryAssessments.count(_.category == 1)
    if (category1AssessmentsCount > 0) {
      answers.getPageValue(
        AssessmentPage(recordId, category1AssessmentsCount - 1)
      ) match {
        case Right(AssessmentAnswer.NoExemption) => CATEGORY_1
        case Right(_)                            => chooseCategory2Or3(recordId, answers, categorisationInfo)
        case _                                   => CATEGORY_1
      }
    } else {
      chooseCategory2Or3(recordId, answers, categorisationInfo)
    }
  }

  private def getHowManyAssessmentsHadExemptions(
    recordId: String,
    answers: UserAnswers,
    categorisationInfo: CategorisationInfo
  ): Int =
    categorisationInfo.categoryAssessments.zipWithIndex
      .map(assessment => answers.get(AssessmentPage(recordId, assessment._2)))
      .count(optionalAnswer => optionalAnswer.isDefined && !optionalAnswer.contains(NoExemption) && !optionalAnswer.contains(NotAnsweredYet))

  private def getMeasurementUnit(answers: UserAnswers, recordId: String): EitherNec[ValidationError, Option[String]] =
    answers
      .get(RecordCategorisationsQuery)
      .map { recordCategorisations =>
        recordCategorisations.records
          .get(recordId)
          .map { categorisationInfo =>
            Right(categorisationInfo.measurementUnit)
          }
          .getOrElse(Left(NonEmptyChain.one(RecordIdMissing(RecordCategorisationsQuery))))
      }
      .getOrElse(Left(NonEmptyChain.one(PageMissing(RecordCategorisationsQuery))))
}
