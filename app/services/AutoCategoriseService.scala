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

import connectors.GoodsRecordConnector
import logging.Logging
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import models.{CategoryRecord, CategoryRecordBuildFailure, Scenario, UserAnswers, UserAnswersSetFailure}
import queries.CategorisationDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AutoCategoriseService @Inject() (
  categorisationService: CategorisationService,
  goodsRecordsConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def autoCategoriseRecord(
    recordId: String,
    userAnswers: UserAnswers
  )(implicit request: DataRequest[_], hc: HeaderCarrier): Future[Option[Scenario]] =
    goodsRecordsConnector.getRecord(recordId).flatMap { record =>
      autoCategoriseRecord(record, userAnswers)
    }

  def autoCategoriseRecord(
    record: GetGoodsRecordResponse,
    userAnswers: UserAnswers
  )(implicit
    request: DataRequest[_],
    hc: HeaderCarrier
  ): Future[Option[Scenario]] =
    categorisationService
      .getCategorisationInfo(request, record.comcode, record.countryOfOrigin, record.recordId)
      .flatMap { categorisationInfo =>
        if (categorisationInfo.isAutoCategorisable && record.category.isEmpty) {

          println("above")

          val updatedUserAnswers =
            userAnswers.set(CategorisationDetailsQuery(record.recordId), categorisationInfo) match {
              case Success(userAnswers) =>
                sessionRepository.set(userAnswers).map(_ => userAnswers)
              case Failure(exception)   => Future.failed(UserAnswersSetFailure(exception.getMessage))
            }

          println("below")

          updatedUserAnswers.flatMap { updatedUserAnswers =>
            CategoryRecord.build(updatedUserAnswers, record.eori, record.recordId, categorisationService) match {
              case Right(record) =>
                // TODO - Probably want to have a audit event for auto categorisation for monitoring

                println("Here")
                for {
                  oldRecord <-
                    goodsRecordsConnector.getRecord(
                      record.recordId
                    )
                  _         <- goodsRecordsConnector.updateCategoryAndComcodeForGoodsRecord(record.recordId, record, oldRecord)
                } yield Some(record.category)

              case Left(errors) =>
                val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")
                Future.failed(CategoryRecordBuildFailure(errorMessages))
            }
          }
        } else {
          println("none case")

          Future.successful(None)
        }
      }

}
