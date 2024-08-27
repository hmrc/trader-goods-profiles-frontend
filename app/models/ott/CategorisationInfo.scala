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
import pages.{AssessmentPage, ReassessmentPage}
import play.api.libs.json.{Json, OFormat}
import utils.Constants.minimumLengthOfCommodityCode

final case class CategorisationInfo(
  commodityCode: String,
  categoryAssessments: Seq[CategoryAssessment],
  categoryAssessmentsThatNeedAnswers: Seq[CategoryAssessment],
  measurementUnit: Option[String],
  descendantCount: Int,
  longerCode: Boolean = false,
  isTraderNiphlsAuthorised: Boolean = false
) {

  def getAssessmentFromIndex(index: Int): Option[CategoryAssessment] =
    if (index + 1 > categoryAssessmentsThatNeedAnswers.size) {
      None
    } else {
      Some(categoryAssessmentsThatNeedAnswers(index))
    }

  def getAnswersForQuestions(userAnswers: UserAnswers, recordId: String): Seq[AnsweredQuestions] =
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
    categoryAssessmentsThatNeedAnswers.zipWithIndex.map(assessment =>
      AnsweredQuestions(
        assessment._2,
        assessment._1,
        userAnswers.get(ReassessmentPage(recordId, assessment._2)),
        reassessmentQuestion = true
      )
    )

  def getMinimalCommodityCode: String =
    commodityCode.reverse.dropWhile(char => char == '0').reverse.padTo(minimumLengthOfCommodityCode, '0').mkString

  def isNiphlsAssessment: Boolean = categoryAssessments.exists(ass => ass.isCategory1 && ass.isNiphlsAnswer) &&
    categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoAnswers)
}

object CategorisationInfo {

  def build(
    ott: OttResponse,
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

        val category1ToAnswer = category1Assessments.filter(ass => !ass.hasNoAnswers).filter(ass => !ass.isNiphlsAnswer)
        val category2ToAnswer = category2Assessments.filter(ass => !ass.hasNoAnswers)

        val areAllCategory1Answerable = category1ToAnswer.size == category1Assessments.size
        val areAllCategory2Answerable = category2ToAnswer.size == category2Assessments.size

        val isNiphlsAssessment =
          category1Assessments.exists(ass => ass.isNiphlsAnswer) && category2Assessments.exists(ass => ass.hasNoAnswers)

        val questionsToAnswer = {
          if (isNiphlsAssessment && traderProfile.niphlNumber.isDefined) {
            category1ToAnswer
          } else if (isNiphlsAssessment) {
            Seq.empty
          } else if (!areAllCategory1Answerable) {
            Seq.empty
          } else if (!areAllCategory2Answerable) {
            category1ToAnswer
          } else {
            category1ToAnswer ++ category2ToAnswer
          }
        }

        CategorisationInfo(
          commodityCodeUserEntered,
          assessmentsSorted,
          questionsToAnswer,
          ott.goodsNomenclature.measurementUnit,
          ott.descendents.size,
          longerCode,
          traderProfile.niphlNumber.isDefined
        )
      }

  implicit lazy val format: OFormat[CategorisationInfo] = Json.format
}
