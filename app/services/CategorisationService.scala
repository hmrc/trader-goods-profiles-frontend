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
import models.{RecordCategorisations, UserAnswers}
import models.ott.CategorisationInfo
import models.requests.DataRequest
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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

    recordCategorisations.records.get(recordId) match {
      case Some(_) =>
        Future.successful(request.userAnswers)
      case None    =>
        for {
          getGoodsRecordResponse <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
          goodsNomenclature      <- ottConnector.getCategorisationInfo(getGoodsRecordResponse.commodityCode)
          categorisationInfo     <- Future.fromTry(Try(CategorisationInfo.build(goodsNomenclature).get))
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
  }
}
