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
import base.TestConstants.testRecordId
import connectors.{GoodsRecordConnector, OttConnector}
import models.RecordCategorisations
import models.ott.CategorisationInfo
import models.ott.response.{CategoryAssessmentRelationship, Descendant, GoodsNomenclatureResponse, IncludedElement, OttResponse}
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategorisationServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockSessionRepository     = mock[SessionRepository]
  private val mockOttConnector          = mock[OttConnector]
  private val mockGoodsRecordsConnector = mock[GoodsRecordConnector]

  private def mockOttResponse(comCode: String = "some comcode") = OttResponse(
    GoodsNomenclatureResponse("some id", comCode, Some("some measure unit"), Instant.EPOCH, None, "test"),
    Seq[CategoryAssessmentRelationship](),
    Seq[IncludedElement](),
    Seq[Descendant]()
  )

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "comcode",
    "countryOfOrigin",
    "traderRef",
    "goodsDescription",
    "adviceStatus",
    Instant.now(),
    Instant.now(),
    "adviceStatus",
    1
  )

  private val categorisationService =
    new CategorisationService(mockSessionRepository, mockOttConnector, mockGoodsRecordsConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
    when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(mockOttResponse()))
    when(mockGoodsRecordsConnector.getRecord(any(), any())(any()))
      .thenReturn(Future.successful(mockGoodsRecordResponse))
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockSessionRepository)
    reset(mockGoodsRecordsConnector)
    reset(mockOttConnector)
  }

  "requireCategorisation" - {

    "should store category assessments if they are not present, then return successful updated answers" in {
      val mockDataRequest               = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)
      val expectedCategorisationInfo    =
        CategorisationInfo("some comcode", Seq(), Some("some measure unit"), 0, Some("comcode"))
      val expectedRecordCategorisations =
        RecordCategorisations(records = Map(testRecordId -> expectedCategorisationInfo))

      val result                        = await(categorisationService.requireCategorisation(mockDataRequest, testRecordId))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should call the router to get the goods record") {
        verify(mockGoodsRecordsConnector, times(1)).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info") {
        verify(mockOttConnector, times(1)).getCategorisationInfo(any(), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "should not call for category assessments if they are already present, then return successful updated answers" in {
      val expectedRecordCategorisations =
        RecordCategorisations(Map("recordId" -> CategorisationInfo("comcode", Seq(), Some("some measure unit"), 0)))

      val userAnswers                   = emptyUserAnswers
        .set(RecordCategorisationsQuery, expectedRecordCategorisations)
        .success
        .value

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val result = await(categorisationService.requireCategorisation(mockDataRequest, "recordId"))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should not call the router to get the goods record") {
        verify(mockGoodsRecordsConnector, never()).getRecord(any(), any())(any())
      }

      withClue("Should not call OTT to get categorisation info") {
        verify(mockOttConnector, never()).getCategorisationInfo(any(), any(), any(), any(), any(), any())(any())
      }

      withClue("Should not call session repository to update user answers") {
        verify(mockSessionRepository, never()).set(any())
      }
    }

    "should return future failed when the call to session repository fails" in {
      reset(mockSessionRepository)
      val expectedException = new RuntimeException("Failed communicating with session repository")
      when(mockSessionRepository.set(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.requireCategorisation(mockDataRequest, "recordId")
        await(result)
      }

      actualException mustBe expectedException
    }

    "should return future failed when the call to the router fails" in {
      reset(mockGoodsRecordsConnector)
      val expectedException = new RuntimeException("Failed communicating with the router")
      when(mockGoodsRecordsConnector.getRecord(any(), any())(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.requireCategorisation(mockDataRequest, "recordId")
        await(result)
      }

      actualException mustBe expectedException
    }

    "should return future failed when the call to OTT fails" in {
      reset(mockOttConnector)
      val expectedException = new RuntimeException("Failed communicating with OTT")
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.requireCategorisation(mockDataRequest, "recordId")
        await(result)
      }

      actualException mustBe expectedException
    }

  }

  "updateCategorisationWithNewCommodityCode" - {

    "should store category assessments if they are not present, then return successful updated answers" in {

      val newCommodity = testCommodity.copy(commodityCode = "newComCode")

      val mockDataRequest = mock[DataRequest[AnyContent]]
      val userAnswers     = emptyUserAnswers.set(LongerCommodityQuery(testRecordId), newCommodity).success.value
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val expectedCategorisationInfo    =
        CategorisationInfo("some comcode", Seq(), Some("some measure unit"), 0, Some("comcode"))
      val expectedRecordCategorisations =
        RecordCategorisations(records = Map(testRecordId -> expectedCategorisationInfo))

      val result                        = await(categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should not need to call the goods record connector") {
        verify(mockGoodsRecordsConnector, times(1)).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info for new commodity code") {
        verify(mockOttConnector, times(1)).getCategorisationInfo(eqTo("newComCode"), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "should replace existing category assessments if they are already present, then return successful updated answers" in {
      val initialRecordCategorisations =
        RecordCategorisations(
          Map(testRecordId -> CategorisationInfo("initialComCode", Seq(), Some("some measure unit"), 0))
        )
      val newCommodity                 = testCommodity.copy(commodityCode = "newComCode")

      when(mockOttConnector.getCategorisationInfo(eqTo("newComCode"), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(mockOttResponse("newComCode")))

      val expectedRecordCategorisations =
        RecordCategorisations(
          Map(testRecordId -> CategorisationInfo("newComCode", Seq(), Some("some measure unit"), 0, Some("comcode")))
        )

      val userAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, initialRecordCategorisations)
        .success
        .value
        .set(LongerCommodityQuery(testRecordId), newCommodity)
        .success
        .value

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val result = await(categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should call OTT to get categorisation info") {
        verify(mockOttConnector, times(1)).getCategorisationInfo(any(), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "should return future failed when the call to session repository fails" in {
      val expectedException = new RuntimeException("Failed communicating with session repository")
      when(mockSessionRepository.set(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]

      val userAnswers = emptyUserAnswers.set(LongerCommodityQuery(testRecordId), testCommodity).success.value
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId)
        await(result)
      }

      actualException mustBe expectedException
    }

    "should return future failed when the longer commodity query is not set" in {

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(emptyUserAnswers)

      intercept[RuntimeException] {
        val result = categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId)
        await(result)
      }

    }

    "should return future failed when the call to the router fails" in {
      val expectedException = new RuntimeException("Failed communicating with the router")
      when(mockGoodsRecordsConnector.getRecord(any(), any())(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      val userAnswers     = emptyUserAnswers.set(LongerCommodityQuery(testRecordId), testCommodity).success.value
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId)
        await(result)
      }

      actualException mustBe expectedException
    }

    "should return future failed when the call to OTT fails" in {
      val expectedException = new RuntimeException("Failed communicating with OTT")
      when(mockOttConnector.getCategorisationInfo(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(expectedException))

      val mockDataRequest = mock[DataRequest[AnyContent]]
      val userAnswers     = emptyUserAnswers.set(LongerCommodityQuery(testRecordId), testCommodity).success.value
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val actualException = intercept[RuntimeException] {
        val result = categorisationService.updateCategorisationWithNewCommodityCode(mockDataRequest, testRecordId)
        await(result)
      }

      actualException mustBe expectedException
    }

  }

}
