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
import models.ott.{CategorisationInfo, CategoryAssessment}
import models.requests.DataRequest
import models.{AssessmentAnswer, RecordCategorisations, UserAnswers}
import pages.{AssessmentPage, InconsistentUserAnswersException}
import queries.{CommodityUpdateQuery, LongerCommodityQuery, RecordCategorisationsQuery}
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

  def requireCategorisation(request: DataRequest[_], recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] = {

    val recordCategorisations =
      request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))

    val originalCommodityCodeOpt =
      recordCategorisations.records.get(recordId).flatMap(_.originalCommodityCode)

    recordCategorisations.records.get(recordId) match {
      case Some(record) =>
        if (originalCommodityCodeOpt.isEmpty) {
          val updatedRecord                = record.copy(originalCommodityCode = Some(record.commodityCode))
          val updatedRecordsMap            = recordCategorisations.records + (recordId -> updatedRecord)
          val updatedRecordCategorisations = RecordCategorisations(updatedRecordsMap)

          for {
            userAnswersWithOriginal <-
              Future.fromTry(request.userAnswers.set(RecordCategorisationsQuery, updatedRecordCategorisations))
            _                       <- sessionRepository.set(userAnswersWithOriginal)
          } yield userAnswersWithOriginal
        } else {
          Future.successful(request.userAnswers)
        }

      case None =>
        for {
          getGoodsRecordResponse           <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
          goodsNomenclature                <- ottConnector.getCategorisationInfo(
                                                getGoodsRecordResponse.comcode,
                                                request.eori,
                                                request.affinityGroup,
                                                Some(recordId),
                                                getGoodsRecordResponse.countryOfOrigin,
                                                LocalDate.now()
                                              )
          originalCommodityCode             = originalCommodityCodeOpt.getOrElse(getGoodsRecordResponse.comcode)
          categorisationInfo               <- CategorisationInfo.build(goodsNomenclature, Some(originalCommodityCode)) match {
                                                case Some(categorisationInfo) => Future.successful(categorisationInfo)
                                                case _                        => Future.failed(new RuntimeException("Could not build categorisation info"))
                                              }
          updatedAnswers                   <-
            Future.fromTry(
              request.userAnswers.set(
                RecordCategorisationsQuery,
                recordCategorisations.copy(records = recordCategorisations.records + (recordId -> categorisationInfo))
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

    val recordCategorisations =
      request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))

    val originalCommodityCodeOpt =
      recordCategorisations.records.get(recordId).flatMap(_.originalCommodityCode)

    for {
      newCommodityCode       <- Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery(recordId)).get))
      getGoodsRecordResponse <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      goodsNomenclature      <- ottConnector.getCategorisationInfo(
                                  newCommodityCode.commodityCode,
                                  request.eori,
                                  request.affinityGroup,
                                  Some(recordId),
                                  getGoodsRecordResponse.countryOfOrigin,
                                  LocalDate.now()
                                )
      originalCommodityCode   = originalCommodityCodeOpt.getOrElse(getGoodsRecordResponse.comcode)
      categorisationInfo     <- CategorisationInfo.build(goodsNomenclature, Some(originalCommodityCode)) match {
                                  case Some(categorisationInfo) => Future.successful(categorisationInfo)
                                  case _                        => Future.failed(new RuntimeException("Could not build categorisation info"))
                                }
      updatedAnswers         <-
        Future.fromTry(
          request.userAnswers.set(
            RecordCategorisationsQuery,
            recordCategorisations.copy(records = recordCategorisations.records + (recordId -> categorisationInfo))
          )
        )
      _                      <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
  }

  def updateCategorisationWithUpdatedCommodityCode(
    request: DataRequest[_],
    recordId: String
  )(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] = {

    val recordCategorisations =
      request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))

    for {
      newCommodityCode                 <- Future.fromTry(Try(request.userAnswers.get(CommodityUpdateQuery(recordId)).get))
      getGoodsRecordResponse           <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      goodsNomenclature                <- ottConnector.getCategorisationInfo(
                                            newCommodityCode.commodityCode,
                                            request.eori,
                                            request.affinityGroup,
                                            Some(recordId),
                                            getGoodsRecordResponse.countryOfOrigin,
                                            LocalDate.now()
                                          )
      categorisationInfo               <- Future.fromTry(Try(CategorisationInfo.build(goodsNomenclature).get))
      updatedAnswers                   <-
        Future.fromTry(
          request.userAnswers.set(
            RecordCategorisationsQuery,
            recordCategorisations.copy(records = recordCategorisations.records + (recordId -> categorisationInfo))
          )
        )
      updatedAnswersCleanUpAssessments <-
        Future.fromTry(cleanupOldAssessmentAnswers(updatedAnswers, recordId))
      _                                <- sessionRepository.set(updatedAnswersCleanUpAssessments)
    } yield updatedAnswersCleanUpAssessments
  }

  def cleanupOldAssessmentAnswers(
    userAnswers: UserAnswers,
    recordId: String
  ): Try[UserAnswers] =
    (for {
      recordQuery        <- userAnswers.get(RecordCategorisationsQuery)
      categorisationInfo <- recordQuery.records.get(recordId)
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
