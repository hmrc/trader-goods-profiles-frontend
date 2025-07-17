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
import models.*
import models.ott.CategorisationInfo
import models.requests.DataRequest
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import queries.LongerCategorisationDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class CategorisationService @Inject() (
                                        ottConnector: OttConnector,
                                        profileConnector: TraderProfileConnector,
                                        sessionRepository: SessionRepository
                                      )(implicit ec: ExecutionContext)
  extends Logging {

  def existsUnansweredCat1Questions(userAnswers: UserAnswers, recordId: String): Boolean =
    userAnswers.get(LongerCategorisationDetailsQuery(recordId)).exists { catInfo =>
      catInfo.categoryAssessmentsThatNeedAnswers.zipWithIndex.exists { case (assessment, index) =>
        val maybeAnswer = userAnswers.get(ReassessmentPage(recordId, index))

        assessment.isCategory1 && (maybeAnswer.isEmpty || maybeAnswer
          .exists(_.answer == AssessmentAnswer.NotAnsweredYet))
      }
    }

  def reorderRecategorisationAnswers(originalUserAnswers: UserAnswers, recordId: String): Future[UserAnswers] =

    for {
      longerCatQuery                 <- Future.fromTry(Try(originalUserAnswers.get(LongerCategorisationDetailsQuery(recordId)).get))
      assessmentsThatNeedAnswers      = longerCatQuery.categoryAssessmentsThatNeedAnswers
      (cat1, cat2)                    = assessmentsThatNeedAnswers.zipWithIndex.partition(_._1.isCategory1)
      cat2AnswersAndIndexes           =
        cat2.flatMap(assessment =>
          originalUserAnswers.get(ReassessmentPage(recordId, assessment._2)).map(_ -> assessment._2)
        )
      (answeredCat2, notAnsweredCat2) = cat2AnswersAndIndexes.partition(_._1.answer != AssessmentAnswer.NotAnsweredYet)
      cat1AnswersAndIndexes           =
        cat1.flatMap(assessment =>
          originalUserAnswers.get(ReassessmentPage(recordId, assessment._2)).map(_ -> assessment._2)
        )
      (answeredCat1, notAnsweredCat1) = cat1AnswersAndIndexes.partition(_._1.answer != AssessmentAnswer.NotAnsweredYet)
      reorderedAnswers                = answeredCat1 ++ notAnsweredCat1 ++ answeredCat2 ++ notAnsweredCat2
      partialReorderedAssessments     = reorderedAnswers.map(_._2).map(assessmentsThatNeedAnswers)
      reorderedAssessments            =
        partialReorderedAssessments ++ assessmentsThatNeedAnswers.filterNot(partialReorderedAssessments.contains)
      newLongerCatQuery               = longerCatQuery.copy(categoryAssessmentsThatNeedAnswers = reorderedAssessments)
      updatedUserAnswers             <-
        Future.fromTry(originalUserAnswers.set(LongerCategorisationDetailsQuery(recordId), newLongerCatQuery))
      updatedUserAnswers             <- Future.fromTry(Try {
        reorderedAnswers.zipWithIndex.reverse.foldLeft(updatedUserAnswers) {
          case (answers, (answerWithIndex, newIndex)) =>
            answers.set(ReassessmentPage(recordId, newIndex), answerWithIndex._1).get
        }
      })
      _                              <- sessionRepository.set(updatedUserAnswers)
    } yield updatedUserAnswers

  def getCategorisationInfo(
                             request: DataRequest[_],
                             commodityCode: String,
                             country: String,
                             recordId: String,
                             longerCode: Boolean = false
                           )(implicit hc: HeaderCarrier): Future[CategorisationInfo] =
    profileConnector.getTraderProfile.flatMap { profile =>
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
          CategorisationInfo.build(response, country, commodityCode, profile) match {  // <--- use country here
            case Right(categorisationInfo) =>
              Future.successful(categorisationInfo)

            case Left(error) =>
              logger.error(s"Could not build categorisation info: $error")
              Future.failed(new RuntimeException("Could not build categorisation info"))
          }
        }
    }


  def calculateResult(
                       categorisationInfo: CategorisationInfo,
                       userAnswers: UserAnswers,
                       recordId: String
                     ): Scenario = {

    val getFirstNo = categorisationInfo
      .getAnswersForQuestions(userAnswers, recordId)
      .find(x => x.answer.contains(AssessmentAnswer.NoExemption))

    val shouldBeCategory1NoExemption: Boolean = categorisationInfo.categoryAssessments.exists(ass =>
      ass.isCategory1 && ass.hasNoExemptions
    ) || categorisationInfo.categoryAssessments.exists(ass =>
      ass.onlyContainsNiphlAnswer && !categorisationInfo.isTraderNiphlAuthorised
    )

    val shouldBeCategory2NoExemption: Boolean = categorisationInfo.categoryAssessments.exists(ass =>
      ass.isCategory2 && ass.hasNoExemptions
    ) || categorisationInfo.categoryAssessments.exists(ass =>
      ass.onlyContainsNirmsAnswer && !categorisationInfo.isTraderNirmsAuthorised
    )

    if (categorisationInfo.categoryAssessments.isEmpty) {
      StandardGoodsNoAssessmentsScenario
    } else {
      getFirstNo match {
        case None if shouldBeCategory1NoExemption          => Category1NoExemptionsScenario
        case None if shouldBeCategory2NoExemption          => Category2NoExemptionsScenario
        case None                                          => StandardGoodsScenario
        case Some(details) if details.question.isCategory2 => Category2Scenario
        case _                                             => Category1Scenario
      }
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

    val uaWithPlaceholders = newAssessments.zipWithIndex.foldLeft[Try[UserAnswers]](Success(userAnswers)) {
      (currentAnswers, newAssessment) =>
        currentAnswers.flatMap(
          _.set(ReassessmentPage(recordId, newAssessment._2), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
        )
    }

    val answersToKeepSortedByNewIndex = listOfAnswersToKeep.toSeq.sortBy(_._1)
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
