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

package services

import connectors.{OttConnector, TraderProfileConnector}
import logging.Logging
import models._
import models.ott.CategorisationInfo
import models.requests.DataRequest
import pages.{AssessmentPage, ReassessmentPage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class CategorisationService @Inject() (
  ottConnector: OttConnector,
  profileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def getCategorisationInfo(
    request: DataRequest[_],
    commodityCode: String,
    country: String,
    recordId: String,
    longerCode: Boolean = false
  )(implicit hc: HeaderCarrier): Future[CategorisationInfo] =
    profileConnector.getTraderProfile(request.eori).flatMap { profile =>
      ottConnector
        .getCategorisationInfo(
          commodityCode,
          request.eori,
          request.affinityGroup,
          Some(recordId),
          country,
          LocalDate.now()
        )
        .flatMap { response =>
          CategorisationInfo.build(response, country, commodityCode, profile, longerCode) match {
            case Some(categorisationInfo) =>
              Future.successful(categorisationInfo)

            case None =>
              logger.error("Could not build categorisation info")
              Future.failed(new RuntimeException("Could not build categorisation info"))
          }
        }
    }

  def calculateResult(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ): Scenario = {
    val category1Assessments = categorisationInfo.categoryAssessments.filter(ass => ass.isCategory1)

    val listOfAnswers = categorisationInfo.getAnswersForQuestions(userAnswers, recordId)

    val areThereCategory1QuestionsWithNoExemption =
      listOfAnswers.exists(x => x.answer.contains(AssessmentAnswer.NoExemption) && x.question.isCategory1)

    val areThereCategory1QuestionsWithNoPossibleAnswers = category1Assessments.exists(_.hasNoAnswers)

    if (categorisationInfo.isNiphlAssessment) {
      if (!categorisationInfo.isTraderNiphlAuthorised || areThereCategory1QuestionsWithNoExemption) {
        Category1Scenario
      } else if (areThereCategory1QuestionsWithNoPossibleAnswers) {
        Category1NoExemptionsScenario
      } else {
        Category2Scenario
      }
    } else {
      calculateResultWithNirms(categorisationInfo, userAnswers, recordId, listOfAnswers)
    }
  }

  private def calculateResultWithNirms(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String,
    listOfAnswers: Seq[AnsweredQuestions]
  ): Scenario = {

    val areThereCategory1AnsweredNo   =
      listOfAnswers.exists(ass => ass.answer.contains(AssessmentAnswer.NoExemption) && ass.question.isCategory1)
    val areThereCategory2AnsweredNo   =
      listOfAnswers.exists(ass => ass.answer.contains(AssessmentAnswer.NoExemption) && ass.question.isCategory2)
    val areThereCategory1Unanswerable =
      categorisationInfo.categoryAssessments.exists(ass => ass.isCategory1 && ass.hasNoAnswers)
    val areThereCategory2Unanswerable =
      categorisationInfo.categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoAnswers)

    if (categorisationInfo.isNirmsAssessment && !areThereCategory1Unanswerable) {

      if (areThereCategory1AnsweredNo) {
        Category1Scenario
      } else if (
        !categorisationInfo.isTraderNirmsAuthorised || areThereCategory2AnsweredNo || areThereCategory2Unanswerable
      ) {
        Category2Scenario
      } else {
        StandardGoodsScenario
      }
    } else {
      calculateResultWithoutNiphlAndNirms(categorisationInfo, userAnswers, recordId)
    }
  }

  private def calculateResultWithoutNiphlAndNirms(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ): Scenario =
    if (categorisationInfo.categoryAssessments.isEmpty) {
      StandardGoodsNoAssessmentsScenario
    } else if (categorisationInfo.categoryAssessmentsThatNeedAnswers.isEmpty) {
      if (categorisationInfo.categoryAssessments.exists(_.isCategory1)) {
        Category1NoExemptionsScenario
      } else {
        Category2Scenario
      }
    } else {
      calculateBasedOnAnswers(categorisationInfo, userAnswers, recordId)
    }

  private def calculateBasedOnAnswers(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ): Scenario = {

    val getFirstNo = categorisationInfo
      .getAnswersForQuestions(userAnswers, recordId)
      .find(x => x.answer.contains(AssessmentAnswer.NoExemption))

    val areThereCategory2QuestionsWithNoExemption =
      categorisationInfo.categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoAnswers)

    getFirstNo match {
      case None if areThereCategory2QuestionsWithNoExemption => Category2Scenario
      case None                                              => StandardGoodsScenario
      case Some(details) if details.question.isCategory2     => Category2Scenario
      case _                                                 => Category1Scenario
    }
  }

  def updatingAnswersForRecategorisation(
    userAnswers: UserAnswers,
    recordId: String,
    oldCommodityCategorisation: CategorisationInfo,
    newCommodityCategorisation: CategorisationInfo
  ): Try[UserAnswers] = {
    val oldAssessments = oldCommodityCategorisation.categoryAssessmentsThatNeedAnswers
    val newAssessments = newCommodityCategorisation.categoryAssessmentsThatNeedAnswers

    val listOfAnswersToKeep = oldAssessments.zipWithIndex.foldLeft(Map.empty[Int, Option[AssessmentAnswer]]) {
      (currentMap, assessment) =>
        val newAssessmentsTheAnswerAppliesTo =
          newAssessments.filter(newAssessment => newAssessment.exemptions == assessment._1.exemptions)
        newAssessmentsTheAnswerAppliesTo.foldLeft(currentMap) { (current, matchingAssessment) =>
          current + (newAssessments.indexOf(matchingAssessment) -> userAnswers.get(
            AssessmentPage(recordId, assessment._2)
          ))
        }
    }

    // Avoid it getting upset if answers have moved too far
    // This is needed as stored as Json array
    val uaWithPlaceholders = newAssessments.zipWithIndex.foldLeft[Try[UserAnswers]](Success(userAnswers)) {
      (currentAnswers, newAssessment) =>
        currentAnswers.flatMap(
          _.set(ReassessmentPage(recordId, newAssessment._2), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
        )
    }

    val answersToKeepSortedByNewIndex = listOfAnswersToKeep.toSeq.sortBy(_._1)
    // Apply them backwards
    // That way, a NoExemption being set will do the automatic cleanup required by CYA and delete any answers afterwards
    answersToKeepSortedByNewIndex.reverse.foldLeft[Try[UserAnswers]](uaWithPlaceholders) {
      (currentAnswers, answerToKeep) =>
        val assessmentIndex     = answerToKeep._1
        val assessmentAnswerOpt = answerToKeep._2
        assessmentAnswerOpt match {
          case Some(answer) =>
            currentAnswers.flatMap(
              _.set(
                ReassessmentPage(recordId, assessmentIndex),
                ReassessmentAnswer(answer, isAnswerCopiedFromPreviousAssessment = true)
              )
            )
          case None         => currentAnswers
        }
    }
  }
}
