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
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models._
import pages.{AssessmentPage, ReassessmentPage}
import uk.gov.hmrc.http.HeaderCarrier
import logging.Logging
import play.api.mvc.Results.Redirect

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
          CategorisationInfo.build(response, commodityCode, profile, longerCode) match {
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
    //val category1ToAnswer = category1Assessments.filter(ass => !ass.hasNoAnswers).filter(ass => !ass.isNiphlsAnswer)
    // val category2ToAnswer = category2Assessments.filter(ass => !ass.hasNoAnswers)

    val hasNiphlAssessments = category1Assessments.exists(ass => ass.isNiphlsAnswer)

    val category1AssessmentsWithoutNiphl = category1Assessments.filter(ass => !ass.isNiphlsAnswer)

    val areThereCategory1QuestionsWithNoExemption = category1Assessments.exists(ass => ass.hasNoAnswers)

    // Let's write unit tests and come back to this later :)
    if (categorisationInfo.isNiphlAuthorised) {
      if (hasNiphlAssessments) {
        if (category1AssessmentsWithoutNiphl.isEmpty) {
          Category2Scenario //scenario 2
        } else if (!areThereCategory1QuestionsWithNoExemption) {
          Category2Scenario //scenario 1
        } else {
          calculateResultWithoutNiphl(categorisationInfo, userAnswers, recordId)
        }
      } else {
        calculateResultWithoutNiphl(categorisationInfo, userAnswers, recordId)
      }
    } else if (hasNiphlAssessments) {
      Category1Scenario //scenario 3
    } else {
      calculateResultWithoutNiphl(categorisationInfo, userAnswers, recordId)
    }

//    if (categorisationInfo.categoryAssessments.isEmpty) {
//      StandardGoodsNoAssessmentsScenario
//    } else if (categorisationInfo.categoryAssessmentsThatNeedAnswers.isEmpty) {
//      if (categorisationInfo.categoryAssessments.exists(_.isCategory1)) {
//        Category1NoExemptionsScenario
//      } else {
//        Category2Scenario
//      }
//    } else {
//      calculateBasedOnAnswers(categorisationInfo, userAnswers, recordId)
//    }
  }

  private def calculateResultWithoutNiphl(
    categorisationInfo: CategorisationInfo,
    userAnswers: UserAnswers,
    recordId: String
  ) =
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
  ) = {
    val listOfAnswers = categorisationInfo.getAnswersForQuestions(userAnswers, recordId)

    val getFirstNo                                = listOfAnswers.find(x => x.answer.contains(AssessmentAnswer.NoExemption))
    val areThereCategory2QuestionsWithNoExemption =
      categorisationInfo.categoryAssessments.exists(ass => ass.isCategory2 && ass.hasNoAnswers)

    getFirstNo match {
      case None if areThereCategory2QuestionsWithNoExemption => Category2Scenario
      case None                                              => StandardGoodsScenario
      case Some(details) if details.question.category == 2   => Category2Scenario
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
        currentAnswers.flatMap(_.set(ReassessmentPage(recordId, newAssessment._2), AssessmentAnswer.NotAnsweredYet))
    }

    val answersToKeepSortedByNewIndex = listOfAnswersToKeep.toSeq.sortBy(_._1)
    // Apply them backwards
    // That way, a NoExemption being set will do the automatic cleanup required by CYA and delete any answers afterwards
    answersToKeepSortedByNewIndex.reverse.foldLeft[Try[UserAnswers]](uaWithPlaceholders) {
      (currentAnswers, answerToKeep) =>
        val assessmentIndex     = answerToKeep._1
        val assessmentAnswerOpt = answerToKeep._2
        assessmentAnswerOpt match {
          case Some(answer) => currentAnswers.flatMap(_.set(ReassessmentPage(recordId, assessmentIndex), answer))
          case None         => currentAnswers
        }
    }
  }
}
