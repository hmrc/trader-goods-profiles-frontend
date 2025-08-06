/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.GoodsRecordConnector
import models.UpdateGoodsRecord
import models.router.requests.PutRecordRequest
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.apache.pekko.Done
import services.GoodsRecordUpdateService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordUpdateServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
  val service                             = new GoodsRecordUpdateService(mockConnector)

  val recordId = "record-123"
  val oldValue = "Old"
  val newValue = "New"

  val updateGoodsRecord: UpdateGoodsRecord = UpdateGoodsRecord(
    eori = "GB123456789000",
    recordId = recordId,
    countryOfOrigin = Some("GB"),
    goodsDescription = Some("Sample goods"),
    productReference = Some("PROD-REF-001"),
    commodityCode = None,
    category = Some(1),
    commodityCodeStartDate = None,
    commodityCodeEndDate = None
  )

  val oldRecord = GetGoodsRecordResponse(
    recordId = recordId,
    eori = "GB123456789000",
    actorId = "actor-1",
    traderRef = "trader-ref-1",
    comcode = "1234567890",
    adviceStatus = null,
    goodsDescription = "Old goods description",
    countryOfOrigin = "GB",
    category = Some(1),
    assessments = None,
    supplementaryUnit = Some(BigDecimal(10)),
    measurementUnit = Some("kg"),
    comcodeEffectiveFromDate = Instant.now().minusSeconds(7200),
    comcodeEffectiveToDate = None,
    version = 1,
    active = true,
    toReview = false,
    reviewReason = None,
    declarable = null,
    ukimsNumber = None,
    nirmsNumber = None,
    niphlNumber = None,
    createdDateTime = Instant.now().minusSeconds(10000),
    updatedDateTime = Instant.now().minusSeconds(5000)
  )

  "updateIfChanged" should {

    "call patchGoodsRecord if values differ and patch = true" in {
      when(mockConnector.patchGoodsRecord(any[UpdateGoodsRecord])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      val result = service.updateIfChanged(oldValue, newValue, updateGoodsRecord, oldRecord, patch = true)

      whenReady(result) { _ =>
        verify(mockConnector).patchGoodsRecord(eqTo(updateGoodsRecord))(any[HeaderCarrier]())
      }
    }

    "call putGoodsRecord if values differ and patch = false" in {
      val expectedPutRequest = PutRecordRequest.mapFromUpdateGoodsRecord(updateGoodsRecord, oldRecord)

      when(mockConnector.putGoodsRecord(any[PutRecordRequest], eqTo(recordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      val result = service.updateIfChanged(oldValue, newValue, updateGoodsRecord, oldRecord, patch = false)

      whenReady(result) { _ =>
        verify(mockConnector).putGoodsRecord(eqTo(expectedPutRequest), eqTo(recordId))(any[HeaderCarrier]())
      }
    }

  }
}
