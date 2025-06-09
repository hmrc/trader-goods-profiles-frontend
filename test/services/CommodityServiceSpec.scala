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

import base.SpecBase
import connectors.{GoodsRecordConnector, OttConnector}
import generators.Generators
import models.Commodity
import models.DeclarableStatus.ImmiReady
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.NOT_FOUND
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.Clock
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommodityServiceSpec extends SpecBase with BeforeAndAfterEach with Generators {

  private val mockOttConnector          = mock[OttConnector]
  private val mockGoodsRecordsConnector = mock[GoodsRecordConnector]
  private val commodityService          = new CommodityService(mockOttConnector, mockGoodsRecordsConnector)

  implicit private lazy val hc: HeaderCarrier  = HeaderCarrier()
  private val validCommodity                   = Commodity("170200", List("a comcode"), Clock.now, None)
  private val request: DataRequest[AnyContent] =
    DataRequest[AnyContent](FakeRequest(), "userId", "eori", AffinityGroup.Organisation, emptyUserAnswers)

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "eori",
    "actorId",
    "traderRef",
    "comcode",
    arbitraryAdviceStatus.sample.value,
    goodsDescriptionKey,
    countryOfOriginKey,
    Some(1),
    None,
    None,
    None,
    Instant.now(),
    None,
    1,
    active = true,
    toReview = true,
    None,
    ImmiReady,
    None,
    None,
    None,
    Instant.now(),
    Instant.now()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOttConnector, mockGoodsRecordsConnector)
  }

  "CommodityService" - {
    "isCommodityValid" - {
      "must return the correct value when passed country of origin and the commodity code" in {
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(validCommodity))

        commodityService
          .isCommodityCodeValid(mockGoodsRecordResponse.comcode, mockGoodsRecordResponse.countryOfOrigin)(request, hc)
          .futureValue mustBe true
      }

      "must return false when passed country of origin and the commodity code and NOT_FOUND is returned" in {
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        commodityService
          .isCommodityCodeValid(mockGoodsRecordResponse.comcode, mockGoodsRecordResponse.countryOfOrigin)(request, hc)
          .futureValue mustBe false
      }

      "must return the correct value when passed a recordId" in {
        when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(mockGoodsRecordResponse))
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(validCommodity))

        commodityService.isCommodityCodeValid(mockGoodsRecordResponse.recordId)(request, hc).futureValue mustBe true
      }

      "must return false when passed a recordId and NOT_FOUND is returned" in {
        when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(mockGoodsRecordResponse))
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        commodityService.isCommodityCodeValid(mockGoodsRecordResponse.recordId)(request, hc).futureValue mustBe false
      }
    }
    "fetchRecordValues" - {
      "must return commodity code and country of origin from a goods record" in {
        when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(mockGoodsRecordResponse))

        commodityService
          .fetchRecordValues(mockGoodsRecordResponse.recordId)(request, hc)
          .futureValue mustBe (mockGoodsRecordResponse.comcode, mockGoodsRecordResponse.countryOfOrigin)
      }
    }
    "fetchCommodity" - {
      "must return commodity information if valid commodity code" in {
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(validCommodity))

        commodityService.fetchCommodity("170200", "GB")(request, hc).futureValue mustBe Some(validCommodity)
      }

      "must return none if a not found upstream error response" in {
        when(mockOttConnector.getCommodityCode(any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        commodityService.fetchCommodity("170200", "GB")(request, hc).futureValue mustBe None
      }
    }
  }
}
