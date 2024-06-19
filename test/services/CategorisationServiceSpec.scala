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
import models.ott.CategorisationInfo
import models.ott.response.{CategoryAssessmentRelationship, GoodsNomenclatureResponse, IncludedElement, OttResponse}
import models.requests.DataRequest
import models.RecordCategorisations
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import queries.RecordCategorisationsQuery
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

  private val mockOttResponse = OttResponse(
    GoodsNomenclatureResponse("some id", "some comcode"),
    Seq[CategoryAssessmentRelationship](),
    Seq[IncludedElement]()
  )

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "comcode",
    "countryOfOrigin",
    "traderRef",
    "goodsDescription",
    "adviceStatus",
    Instant.now(),
    Instant.now()
  )

  private val categorisationService =
    new CategorisationService(mockSessionRepository, mockOttConnector, mockGoodsRecordsConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
    when(mockOttConnector.getCategorisationInfo(any())(any())).thenReturn(Future.successful(mockOttResponse))
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
      val expectedCategorisationInfo    = CategorisationInfo("some comcode", Seq())
      val expectedRecordCategorisations = RecordCategorisations(records = Map("recordId" -> expectedCategorisationInfo))

      val result = await(categorisationService.requireCategorisation(mockDataRequest, "recordId"))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should call the router to get the goods record") {
        verify(mockGoodsRecordsConnector, times(1)).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info") {
        verify(mockOttConnector, times(1)).getCategorisationInfo(any())(any())
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "should not call for category assessments if they are already present, then return successful updated answers" in {
      val expectedRecordCategorisations = RecordCategorisations(Map("recordId" -> CategorisationInfo("comcode", Seq())))

      val userAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, expectedRecordCategorisations)
        .success
        .value

      val mockDataRequest = mock[DataRequest[AnyContent]]
      when(mockDataRequest.userAnswers).thenReturn(userAnswers)

      val result = await(categorisationService.requireCategorisation(mockDataRequest, "recordId"))
      result.get(RecordCategorisationsQuery).get mustBe expectedRecordCategorisations

      withClue("Should call the router to get the goods record") {
        verify(mockGoodsRecordsConnector, never()).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info") {
        verify(mockOttConnector, never()).getCategorisationInfo(any())(any())
      }

      withClue("Should call session repository to update user answers") {
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
      when(mockOttConnector.getCategorisationInfo(any())(any()))
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
}
