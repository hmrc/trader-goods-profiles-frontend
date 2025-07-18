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

import com.google.inject.Inject
import connectors.GoodsRecordConnector
import models.UpdateGoodsRecord
import models.router.requests.PutRecordRequest
import models.router.responses.GetGoodsRecordResponse
import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GoodsRecordUpdateService @Inject() (connector: GoodsRecordConnector) {

  def updateIfChanged(
    oldValue: String,
    newValue: String,
    updateGoodsRecord: UpdateGoodsRecord,
    oldRecord: GetGoodsRecordResponse,
    patch: Boolean = true
  )(implicit hc: HeaderCarrier): Future[Done] =
    if (oldValue != newValue) {
      if (patch) {
        connector.patchGoodsRecord(updateGoodsRecord)
      } else {
        val putRecordRequest = PutRecordRequest.mapFromUpdateGoodsRecord(updateGoodsRecord, oldRecord)
        connector.putGoodsRecord(putRecordRequest, updateGoodsRecord.recordId)
      }
    } else {
      Future.successful(Done)
    }
}
