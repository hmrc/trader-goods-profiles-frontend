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

import connectors.{GoodsRecordConnector, OttConnector}
import models.AssessmentAnswer.NotAnsweredYet
import models.ott.{CategorisationInfo, CategorisationInfo2}
import models.requests.DataRequest
import models.{AssessmentAnswer, UserAnswers}
import pages.{AssessmentPage, InconsistentUserAnswersException}
import queries.{CategorisationDetailsQuery, CommodityUpdateQuery, LongerCommodityQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.firstAssessmentIndex

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CategorisationService @Inject() (
  sessionRepository: SessionRepository,
  ottConnector: OttConnector,
  goodsRecordsConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext) {

  // get details from OTT
  // work out what questions need answerin'

  def getCategorisationInfo(
    request: DataRequest[_],
    commodityCode: String,
    country: String,
    recordId: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[CategorisationInfo2] = {

    val ottResponse = ottConnector.getCategorisationInfo(
      commodityCode,
      request.eori,
      request.affinityGroup,
      recordId,
      country,
      LocalDate.now()
    )

    ottResponse.flatMap { response =>
      CategorisationInfo2.build(response) match {
        case Some(categorisationInfo) => Future.successful(categorisationInfo)
        case _                        =>
          Future.failed(new RuntimeException("Could not build categorisation info"))
      }

    }

  }

  def requireCategorisation(request: DataRequest[_], recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] = {

    val currentCategorisationInfo = request.userAnswers.get(CategorisationDetailsQuery(recordId))

    val originalCommodityCodeOpt =
      currentCategorisationInfo.flatMap(_.originalCommodityCode)

    currentCategorisationInfo match {
      case Some(_) =>
        Future.successful(request.userAnswers)
      case None    =>
        for {
          getGoodsRecordResponse           <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
          goodsNomenclature                <- ottConnector.getCategorisationInfo(
                                                getGoodsRecordResponse.comcode,
                                                request.eori,
                                                request.affinityGroup,
                                                Some(recordId),
                                                getGoodsRecordResponse.countryOfOrigin,
                                                LocalDate.now() //TODO where does DateOfTrade come from??
                                              )
          originalCommodityCode             = originalCommodityCodeOpt.getOrElse(getGoodsRecordResponse.comcode)
          newCategorisationInfo            <- CategorisationInfo.build(goodsNomenclature, Some(originalCommodityCode)) match {
                                                case Some(categorisationInfo) => Future.successful(categorisationInfo)
                                                case _                        =>
                                                  Future.failed(new RuntimeException("Could not build categorisation info"))
                                              }
          updatedAnswers                   <-
            Future.fromTry(
              request.userAnswers.set(
                CategorisationDetailsQuery(recordId),
                newCategorisationInfo
              )
            )
          updatedAnswersCleanUpAssessments <-
            Future.fromTry(cleanupOldAssessmentAnswers(updatedAnswers, recordId))
          _                                <- sessionRepository.set(updatedAnswersCleanUpAssessments)
        } yield updatedAnswersCleanUpAssessments
    }
  }

  def updateCategorisationWithNewCommodityCode(
    request: DataRequest[_],
    recordId: String
  )(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] = {

    val currentCategorisationInfo = request.userAnswers.get(CategorisationDetailsQuery(recordId))

    val originalCommodityCodeOpt =
      currentCategorisationInfo.flatMap(_.originalCommodityCode)

    for {
      newCommodityCode       <- Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery(recordId)).get))
      getGoodsRecordResponse <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      goodsNomenclature      <- ottConnector.getCategorisationInfo(
                                  newCommodityCode.commodityCode,
                                  request.eori,
                                  request.affinityGroup,
                                  Some(recordId),
                                  getGoodsRecordResponse.countryOfOrigin,
                                  LocalDate.now() //TODO where does DateOfTrade come from??
                                )
      originalCommodityCode   = originalCommodityCodeOpt.getOrElse(getGoodsRecordResponse.comcode)
      newCategorisationInfo  <- CategorisationInfo.build(goodsNomenclature, Some(originalCommodityCode)) match {
                                  case Some(categorisationInfo) => Future.successful(categorisationInfo)
                                  case _                        => Future.failed(new RuntimeException("Could not build categorisation info"))
                                }
      updatedAnswers         <-
        Future.fromTry(
          request.userAnswers.set(
            CategorisationDetailsQuery(recordId),
            newCategorisationInfo
          )
        )
      _                      <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
  }

  //TODO this will be refactored out in TGP-1600 but solves the immediate problem
  def updateCategorisationWithUpdatedCommodityCode(
    request: DataRequest[_],
    recordId: String
  )(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] =
    for {
      newCommodityCode                 <- Future.fromTry(Try(request.userAnswers.get(CommodityUpdateQuery(recordId)).get))
      getGoodsRecordResponse           <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      goodsNomenclature                <- ottConnector.getCategorisationInfo(
                                            newCommodityCode.commodityCode,
                                            request.eori,
                                            request.affinityGroup,
                                            Some(recordId),
                                            getGoodsRecordResponse.countryOfOrigin,
                                            LocalDate.now() //TODO where does DateOfTrade come from??
                                          )
      newCategorisationInfo            <- Future.fromTry(Try(CategorisationInfo.build(goodsNomenclature).get))
      updatedAnswers                   <-
        Future.fromTry(
          request.userAnswers.set(CategorisationDetailsQuery(recordId), newCategorisationInfo)
        )
      updatedAnswersCleanUpAssessments <-
        Future.fromTry(cleanupOldAssessmentAnswers(updatedAnswers, recordId))
      _                                <- sessionRepository.set(updatedAnswersCleanUpAssessments)
    } yield updatedAnswersCleanUpAssessments

  def cleanupOldAssessmentAnswers(
    userAnswers: UserAnswers,
    recordId: String
  ): Try[UserAnswers] =
    (for {
      categorisationInfo <- userAnswers.get(CategorisationDetailsQuery(recordId))
      count               = categorisationInfo.categoryAssessments.size
      //Go backwards to avoid recursion issues
      rangeToRemove       = (firstAssessmentIndex to count + 1).reverse
    } yield rangeToRemove.foldLeft[Try[UserAnswers]](Success(userAnswers)) { (acc, currentIndexToRemove) =>
      acc.flatMap(_.remove(AssessmentPage(recordId, currentIndexToRemove)))
    }).getOrElse(
      Failure(new InconsistentUserAnswersException(s"Could not find category assessments"))
    )

  def updatingAnswersForRecategorisation(
    userAnswers: UserAnswers,
    recordId: String,
    oldCommodityCategorisation: CategorisationInfo,
    newCommodityCategorisation: CategorisationInfo
  ): Try[UserAnswers] =
    if (oldCommodityCategorisation == newCommodityCategorisation) {
      Success(userAnswers)
    } else {
      val oldAssessments = oldCommodityCategorisation.categoryAssessments
      val newAssessments = newCommodityCategorisation.categoryAssessments

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

      val cleanedUserAnswers = cleanupOldAssessmentAnswers(userAnswers, recordId).get
      // Avoid it getting upset if answers have moved too far
      val uaWithPlaceholders = newAssessments.zipWithIndex.foldLeft[Try[UserAnswers]](Success(cleanedUserAnswers)) {
        (currentAnswers, newAssessment) =>
          currentAnswers.flatMap(_.set(AssessmentPage(recordId, newAssessment._2), NotAnsweredYet))
      }

      val answersToKeepSortedByNewIndex = listOfAnswersToKeep.toSeq.sortBy(_._1)
      // Apply them backwards
      // That way, a NoExemption being set will do the automatic cleanup required by CYA and delete any answers afterwards
      answersToKeepSortedByNewIndex.reverse.foldLeft[Try[UserAnswers]](uaWithPlaceholders) {
        (currentAnswers, answerToKeep) =>
          val assessmentIndex     = answerToKeep._1
          val assessmentAnswerOpt = answerToKeep._2
          assessmentAnswerOpt match {
            case Some(answer) => currentAnswers.flatMap(_.set(AssessmentPage(recordId, assessmentIndex), answer))
            case None         => currentAnswers
          }
      }
    }
}
