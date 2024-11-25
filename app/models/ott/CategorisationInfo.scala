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

package models.ott

import cats.implicits.toTraverseOps
import models.ott.response.OttResponse
import models.{AnsweredQuestions, TraderProfile, UserAnswers}
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import play.api.libs.json.{Json, OFormat}
import utils.Constants.minimumLengthOfCommodityCode

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

final case class CategorisationInfo(
  commodityCode: String,
  countryOfOrigin: String,
  comcodeEffectiveToDate: Option[Instant],
  categoryAssessments: Seq[CategoryAssessment],
  categoryAssessmentsThatNeedAnswers: Seq[CategoryAssessment],
  measurementUnit: Option[String],
  descendantCount: Int,
  longerCode: Boolean = false,
  isTraderNiphlAuthorised: Boolean = false,
  isTraderNirmsAuthorised: Boolean = false
) {

  def getAssessmentFromIndex(index: Int): Option[CategoryAssessment] =
    if (index + 1 > categoryAssessmentsThatNeedAnswers.size) {
      None
    } else {
      Some(categoryAssessmentsThatNeedAnswers(index))
    }

  def getAnswersForQuestions(
    userAnswers: UserAnswers,
    recordId: String
  ): Seq[AnsweredQuestions] =
    if (longerCode) {
      getAnswersForReassessmentQuestions(userAnswers, recordId)
    } else {
      categoryAssessmentsThatNeedAnswers.zipWithIndex.map(assessment =>
        AnsweredQuestions(
          assessment._2,
          assessment._1,
          userAnswers.get(AssessmentPage(recordId, assessment._2))
        )
      )
    }

  private def getAnswersForReassessmentQuestions(userAnswers: UserAnswers, recordId: String): Seq[AnsweredQuestions] =
    categoryAssessmentsThatNeedAnswers.zipWithIndex.map { assessment =>
      val answerOpt = userAnswers.get(ReassessmentPage(recordId, assessment._2))

      AnsweredQuestions(
        assessment._2,
        assessment._1,
        answerOpt.map(_.answer),
        reassessmentQuestion = true,
        wasCopiedFromInitialAssessment = answerOpt.exists(_.isAnswerCopiedFromPreviousAssessment)
      )
    }

  def getMinimalCommodityCode: String =
    commodityCode.reverse.dropWhile(char => char == '0').reverse.padTo(minimumLengthOfCommodityCode, '0').mkString

  def isNiphlAssessment: Boolean = categoryAssessments.exists(ass => ass.isNiphlAnswer) &&
    categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoAnswers)

  def isNirmsAssessment: Boolean = categoryAssessments.exists(_.isNirmsAnswer)

  def isCommCodeExpired: Boolean = {
    val today: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
    comcodeEffectiveToDate.exists { effectiveToDate =>
      val effectiveDate: ZonedDateTime = effectiveToDate.atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS)
      effectiveDate.isEqual(today)

    }

  }
}

object CategorisationInfo {

  def build(
    ott: OttResponse,
    countryOfOrigin: String,
    commodityCodeUserEntered: String,
    traderProfile: TraderProfile,
    longerCode: Boolean = false
  ): Option[CategorisationInfo] =
    ott.categoryAssessmentRelationships
      .map(x => CategoryAssessment.build(x.id, ott))
      .sequence
      .map { assessments =>
        val assessmentsSorted = assessments.sorted

        val category1Assessments = assessmentsSorted.filter(ass => ass.isCategory1)
        val category2Assessments = assessmentsSorted.filter(ass => ass.isCategory2)

        val category1ToAnswer = category1Assessments.filter(ass => !ass.hasNoAnswers).filter(ass => !ass.isNiphlAnswer)
        val category2ToAnswer = category2Assessments.filter(ass => !ass.hasNoAnswers).filter(ass => !ass.isNirmsAnswer)

        val areAllCategory1Answerable = category1ToAnswer.size == category1Assessments.size
        val areAllCategory2Answerable = category2ToAnswer.size == category2Assessments.size

        val isNiphlAssessment =
          category1Assessments.exists(ass => ass.isNiphlAnswer) && category2Assessments.exists(ass => ass.hasNoAnswers)

        val isNirmsAssessment = category2Assessments.exists(ass => ass.isNirmsAnswer)

        val isTraderNiphlAuthorised = traderProfile.niphlNumber.isDefined
        val isTraderNirmsAuthorised = traderProfile.nirmsNumber.isDefined

        val questionsToAnswer = {
          if (isNiphlAssessment && isTraderNiphlAuthorised) {
            category1ToAnswer
          } else if (isNiphlAssessment) {
            Seq.empty
          } else if (!areAllCategory1Answerable) {
            Seq.empty
          } else if (isNirmsAssessment && isTraderNirmsAuthorised) {
            category1ToAnswer ++ category2ToAnswer
          } else if (!areAllCategory2Answerable) {
            category1ToAnswer
          } else {
            category1ToAnswer ++ category2ToAnswer
          }
        }

        CategorisationInfo(
          commodityCodeUserEntered,
          countryOfOrigin,
          ott.goodsNomenclature.validityEndDate,
          assessmentsSorted,
          questionsToAnswer,
          ott.goodsNomenclature.measurementUnit,
          ott.descendents.size,
          longerCode,
          isTraderNiphlAuthorised,
          isTraderNirmsAuthorised
        )
      }

  implicit lazy val format: OFormat[CategorisationInfo] = Json.format
}
