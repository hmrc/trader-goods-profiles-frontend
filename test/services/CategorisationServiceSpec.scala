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
import models.AssessmentAnswer.NotAnsweredYet
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import models.ott.response._
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import models.{AssessmentAnswer, RecordCategorisations}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.AssessmentPage
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
    GoodsNomenclatureResponse("some id", comCode, Some("some measure unit"), Instant.EPOCH, None, List("test")),
    Seq[CategoryAssessmentRelationship](),
    Seq[IncludedElement](),
    Seq[Descendant]()
  )

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "recordId",
    "eori",
    "actorId",
    "traderRef",
    "comcode",
    "adviceStatus",
    "goodsDescription",
    "countryOfOrigin",
    Some(1),
    None,
    None,
    None,
    Instant.now(),
    None,
    1,
    true,
    true,
    None,
    "declarable",
    None,
    None,
    None,
    Instant.now(),
    Instant.now()
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
        verify(mockGoodsRecordsConnector).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info") {
        verify(mockOttConnector).getCategorisationInfo(any(), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository).set(any())
      }
    }

    "should not call for category assessments if they are already present, then return successful updated answers" in {
      val expectedRecordCategorisations =
        RecordCategorisations(
          Map(
            "recordId" -> CategorisationInfo("test-comcode", Seq(), Some("some measure unit"), 0, Some("test-comcode"))
          )
        )

      val userAnswers = emptyUserAnswers
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
        verify(mockGoodsRecordsConnector).getRecord(any(), any())(any())
      }

      withClue("Should call OTT to get categorisation info for new commodity code") {
        verify(mockOttConnector).getCategorisationInfo(eqTo("newComCode"), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository).set(any())
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
        verify(mockOttConnector).getCategorisationInfo(any(), any(), any(), any(), any(), any())(
          any()
        )
      }

      withClue("Should call session repository to update user answers") {
        verify(mockSessionRepository).set(any())
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

  "cleanupOldAssessmentAnswers" - {

    "remove old assessment answers for given recordId" in {

      val initialUserAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, recordCategorisations)
        .success
        .value
        .set(
          AssessmentPage(testRecordId, 0),
          AssessmentAnswer.Exemption("1234")
        )
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("4321"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
        .success
        .value
        .set(
          AssessmentPage("b0082f50-f13b-416a-8071-3bd95107d44e", 0),
          AssessmentAnswer.Exemption("1234")
        )
        .success
        .value

      val result            = categorisationService.cleanupOldAssessmentAnswers(initialUserAnswers, testRecordId)
      val resultUserAnswers = result.get

      resultUserAnswers.get(AssessmentPage(testRecordId, 0)) mustBe None
      resultUserAnswers.get(AssessmentPage(testRecordId, 1)) mustBe None
      resultUserAnswers.get(AssessmentPage(testRecordId, 2)) mustBe None

      withClue("Other record ids must be unaffected") {
        resultUserAnswers.get(AssessmentPage("b0082f50-f13b-416a-8071-3bd95107d44e", 0)) mustBe Some(
          AssessmentAnswer.Exemption("1234")
        )
      }

    }

  }

  "updatingAnswersForRecategorisation" - {

    "should return the same user answers if old and new category assessments are the same" in {
      val result = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categoryQuery,
          categoryQuery
        )
        .success
        .value
      result shouldBe userAnswersForCategorisation
    }

    "should clean up the old answers if all the assessments are different" in {
      val newCommodityCategorisation = CategorisationInfo(
        "12345",
        Seq(CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))),
        None,
        1,
        Some("123")
      )
      val result                     = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categoryQuery,
          newCommodityCategorisation
        )
        .success
        .value
      result.get(AssessmentPage(testRecordId, 0)) shouldBe Some(NotAnsweredYet)
      result.get(AssessmentPage(testRecordId, 1)) shouldBe None
      result.get(AssessmentPage(testRecordId, 2)) shouldBe None
    }

    "should return all the old answers when new category info has a new assessment" in {
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(
          category1,
          category2,
          category3,
          CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
        ),
        Some("Weight, in kilograms"),
        0,
        Some("1234567890")
      )
      val result                     = categorisationService
        .updatingAnswersForRecategorisation(
          userAnswersForCategorisation,
          testRecordId,
          categoryQuery,
          newCommodityCategorisation
        )
        .success
        .value
      result.get(AssessmentPage(testRecordId, 0)) shouldBe Some(AssessmentAnswer.Exemption("Y994"))
      result.get(AssessmentPage(testRecordId, 1)) shouldBe Some(AssessmentAnswer.Exemption("NC123"))
      result.get(AssessmentPage(testRecordId, 2)) shouldBe Some(AssessmentAnswer.Exemption("X812"))
    }

    "should move the old answers to the right position if they are in different order in the new categorisation" in {

      val oldCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category1, category2, category3),
        Some("Weight, in kilograms"),
        0,
        Some("1234567890")
      )

      val oldUserAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, recordCategorisations)
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("NC123"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("X812"))
        .success
        .value

      val category4                  = CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category3, category1, category4, category2),
        Some("Weight, in kilograms"),
        0,
        Some("1234567890")
      )

      val newUserAnswers = categorisationService
        .updatingAnswersForRecategorisation(
          oldUserAnswers,
          testRecordId,
          oldCommodityCategorisation,
          newCommodityCategorisation
        )
        .success
        .value
      newUserAnswers.get(AssessmentPage(testRecordId, 0)) mustBe Some(AssessmentAnswer.Exemption("X812"))
      newUserAnswers.get(AssessmentPage(testRecordId, 1)) mustBe Some(AssessmentAnswer.Exemption("Y994"))
      newUserAnswers.get(AssessmentPage(testRecordId, 2)) mustBe Some(NotAnsweredYet)
      newUserAnswers.get(AssessmentPage(testRecordId, 3)) mustBe Some(AssessmentAnswer.Exemption("NC123"))
    }

    "should move the old answers to the right position if only some are in the new categorisation" in {

      val oldCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category1, category2, category3),
        Some("Weight, in kilograms"),
        0,
        Some("1234567890")
      )

      val oldUserAnswers = emptyUserAnswers
        .set(RecordCategorisationsQuery, recordCategorisations)
        .success
        .value
        .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("NC123"))
        .success
        .value
        .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("X812"))
        .success
        .value

      val category4                  = CategoryAssessment("0", 1, Seq(Certificate("Y199", "Y199", "Goods are not from warzone")))
      val newCommodityCategorisation = CategorisationInfo(
        "1234567890",
        Seq(category1, category4),
        Some("Weight, in kilograms"),
        0,
        Some("1234567890")
      )

      val newUserAnswers = categorisationService
        .updatingAnswersForRecategorisation(
          oldUserAnswers,
          testRecordId,
          oldCommodityCategorisation,
          newCommodityCategorisation
        )
        .success
        .value
      newUserAnswers.get(AssessmentPage(testRecordId, 0)) mustBe Some(AssessmentAnswer.Exemption("Y994"))
      newUserAnswers.get(AssessmentPage(testRecordId, 1)) mustBe Some(NotAnsweredYet)
      newUserAnswers.get(AssessmentPage(testRecordId, 2)) mustBe None
    }

  }

}
