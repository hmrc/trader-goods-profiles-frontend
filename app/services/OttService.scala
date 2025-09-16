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
import models.requests.DataRequest
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class OttService @Inject() (
  ottConnector: OttConnector,
  goodsRecordsConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def getMeasurementUnit(request: DataRequest[_], recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[String]] =
    goodsRecordsConnector.getRecord(recordId).flatMap {
      case Some(getGoodsRecordResponse) =>
        ottConnector
          .getCategorisationInfo(
            getGoodsRecordResponse.comcode,
            request.eori,
            request.affinityGroup,
            Some(recordId),
            getGoodsRecordResponse.countryOfOrigin,
            LocalDate.now()
          )
          .map(_.goodsNomenclature.measurementUnit)
          .recover { case ex: Exception =>
            logger.error(s"Error occurred while fetching measurement unit for recordId: $recordId", ex)
            None
          }
      case None                         =>
        logger.info(s"Record not found for recordId: $recordId")
        Future.successful(None)
    }
}
