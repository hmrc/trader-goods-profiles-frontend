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
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.{RecordCategorisations, UserAnswers}
import pages.{AssessmentPage, InconsistentUserAnswersException}
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
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
  ): Future[UserAnswers] =

    (for {
      getGoodsRecordResponse <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      recordCategorisations =
        request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))
    } yield {

      recordCategorisations.records.get(recordId) match {
        case Some(catInfo) =>

          if (catInfo.commodityCode == getGoodsRecordResponse.comcode) {
            Future.successful(request.userAnswers)
          } else {
            updateCategorisationDetails(request, recordId, recordCategorisations, getGoodsRecordResponse.comcode, getGoodsRecordResponse.countryOfOrigin)
          }

        case None =>
          updateCategorisationDetails(request, recordId, recordCategorisations, getGoodsRecordResponse.comcode, getGoodsRecordResponse.countryOfOrigin)
      }
    }).flatten


  private def updateCategorisationDetails(request: DataRequest[_], recordId: String, recordCategorisations: RecordCategorisations,
                                          commodityCode: String, countryOfOrigin: String)
                                         (implicit hc:HeaderCarrier) = {
    for {
      goodsNomenclature <- ottConnector.getCategorisationInfo(
        commodityCode,
        request.eori,
        request.affinityGroup,
        Some(recordId),
        countryOfOrigin,
        LocalDate.now() //TODO where does DateOfTrade come from??
      )
      originalCommodityCodeOpt =
        recordCategorisations.records.get(recordId).flatMap(_.originalCommodityCode)
      originalCommodityCode             = originalCommodityCodeOpt.getOrElse(getGoodsRecordResponse.comcode)
      categorisationInfo <- CategorisationInfo.build(goodsNomenclature, Some(originalCommodityCode)) match {
        case Some(categorisationInfo) => Future.successful(categorisationInfo)
        case _ => Future.failed(new RuntimeException("Could not build categorisation info"))
      }
      updatedAnswers <-
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

  def updateCategorisationWithLongerCommodityCode(
    request: DataRequest[_],
    recordId: String
  )(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] =

    (for {
      getGoodsRecordResponse <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
      newCommodityCode       <- Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery(recordId)).get))
      recordCategorisations =
        request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))
    } yield updateCategorisationDetails(request, recordId, recordCategorisations, newCommodityCode.commodityCode, getGoodsRecordResponse.countryOfOrigin)).flatten

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
}
