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
import config.Service
import connectors.{GoodsRecordConnector, OttConnector}
import generators.Generators
import models.DeclarableStatus.ImmiReady
import models.ott.response.*
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OttServiceSpec extends SpecBase with BeforeAndAfterEach with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockOttConnector          = mock[OttConnector]
  private val mockGoodsRecordsConnector = mock[GoodsRecordConnector]
  private val ottService                = new OttService(mockOttConnector, mockGoodsRecordsConnector)

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

  private def mockOttResponse(comCode: String = "some comcode") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("some measure unit"), Instant.EPOCH, None, List("test")),
    Seq[CategoryAssessmentRelationship](),
    Seq[IncludedElement](),
    Seq[Descendant]()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(mockGoodsRecordResponse))
    when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(mockOttResponse()))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockOttConnector, mockGoodsRecordsConnector)
  }

  "getMeasurementUnit" - {
    "should return measurement unit when both connectors respond successfully" in {
      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val expectedMeasurementUnit = "some measure unit"
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(mockOttResponse()))

      val result = await(ottService.getMeasurementUnit(mockDataRequest, "recordId"))
      result shouldBe Some(expectedMeasurementUnit)

      withClue("Should call Goods Records Connector") {
        verify(mockGoodsRecordsConnector).getRecord(any())(any())
      }

      withClue("Should call OTT Connector") {
        verify(mockOttConnector).getCategorisationInfo(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "should return None when Goods Records Connector fails" in {
      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Failed to get goods record")))

      val result = await(ottService.getMeasurementUnit(mockDataRequest, "recordId"))
      result shouldBe None
    }

    "should return None when OTT Connector fails" in {
      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Failed to get categorisation info")))

      val result = await(ottService.getMeasurementUnit(mockDataRequest, "recordId"))
      result shouldBe None
    }

    "Service implicit convertToString" - {
      "convert Service to its baseUrl string representation" in {
        val service     = Service("localhost", "9000", "http")
        val url: String = service

        url shouldBe "http://localhost:9000"
      }
    }
  }
}
