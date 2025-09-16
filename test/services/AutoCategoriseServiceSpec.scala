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
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import generators.Generators
import models.*
import models.DeclarableStatus.NotReadyForUse
import models.ott.*
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContent
import queries.CategorisationDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.{countryOfOriginKey, goodsDescriptionKey}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AutoCategoriseServiceSpec extends SpecBase with BeforeAndAfterEach with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  private val mockGoodsRecordsConnector  = mock[GoodsRecordConnector]
  private val mockSessionRepository      = mock[SessionRepository]
  private val mockCategorisationService  = mock[CategorisationService]

  private val mockGoodsRecordResponse = GetGoodsRecordResponse(
    "testRecordId",
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
    NotReadyForUse,
    None,
    None,
    None,
    Instant.now(),
    Instant.now()
  )

  private val traderProfileWithAuthorisation =
    TraderProfile("actorId", "ukims number", Some("NIPHL"), Some("NIRMS"), eoriChanged = false)
  private val autoCategorisationService      =
    new AutoCategoriseService(mockCategorisationService, mockGoodsRecordsConnector, mockSessionRepository)
  private val mockDataRequest                = mock[DataRequest[AnyContent]]

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
    when(mockGoodsRecordsConnector.getRecord(any())(any())).thenReturn(Future.successful(Some(mockGoodsRecordResponse)))
    when(mockTraderProfileConnector.getTraderProfile(any()))
      .thenReturn(Future.successful(traderProfileWithAuthorisation))
    when(mockDataRequest.eori).thenReturn("eori")
    when(mockDataRequest.affinityGroup).thenReturn(AffinityGroup.Individual)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockSessionRepository, mockGoodsRecordsConnector, mockTraderProfileConnector, mockCategorisationService)
  }

  val categoryAssessment: CategoryAssessment = CategoryAssessment(
    id = "assessmentId",
    category = 1,
    exemptions = Seq(Certificate("1", "1", "1")),
    themeDescription = "1",
    None
  )

  "autoCategoriseRecord(recordId, userAnswers)" - {

    "return None when the record is already categorised" in {
      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq.empty,
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = Some(1)))))

      val result =
        autoCategorisationService.autoCategoriseRecord("recordId", userAnswers)(mockDataRequest, hc).futureValue

      result shouldBe None
    }

    "return None when the record is not auto categorisable" in {
      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(categoryAssessment),
        categoryAssessmentsThatNeedAnswers = Seq(categoryAssessment),
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = None))))

      val result =
        autoCategorisationService.autoCategoriseRecord(testRecordId, userAnswers)(mockDataRequest, hc).futureValue

      result shouldBe None
    }

    "return the scenario if update completed" in {
      val categoryAssessment = CategoryAssessment(
        id = "assessmentId",
        category = 1,
        exemptions = Seq.empty,
        themeDescription = "some theme",
        None
      )

      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(categoryAssessment),
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockCategorisationService.calculateResult(any(), any(), any()))
        .thenReturn(StandardGoodsNoAssessmentsScenario)
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = None))))
      when(mockGoodsRecordsConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val result =
        autoCategorisationService.autoCategoriseRecord(testRecordId, userAnswers)(mockDataRequest, hc).futureValue

      result.value shouldBe StandardGoodsNoAssessmentsScenario
    }
  }

  "autoCategoriseRecord(record, userAnswers)" - {
    "return None when the record is already categorised" in {
      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq.empty,
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = Some(1)))))

      val result = autoCategorisationService
        .autoCategoriseRecord(mockGoodsRecordResponse.copy(category = Some(1)), userAnswers)(mockDataRequest, hc)
        .futureValue

      result shouldBe None
    }

    "return None when the record is not auto categorisable" in {
      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(categoryAssessment),
        categoryAssessmentsThatNeedAnswers = Seq(categoryAssessment),
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = None))))

      val result = autoCategorisationService
        .autoCategoriseRecord(mockGoodsRecordResponse, userAnswers)(mockDataRequest, hc)
        .futureValue

      result shouldBe None
    }

    "return the scenario if update completed" in {
      val categoryAssessment = CategoryAssessment(
        id = "assessmentId",
        category = 1,
        exemptions = Seq.empty,
        themeDescription = "some theme",
        None
      )

      val categorisationInfo: CategorisationInfo = CategorisationInfo(
        commodityCode = "1234567890",
        countryOfOrigin = "GB",
        comcodeEffectiveToDate = None,
        categoryAssessments = Seq(categoryAssessment),
        categoryAssessmentsThatNeedAnswers = Seq.empty,
        measurementUnit = None,
        descendantCount = 0
      )

      val userAnswers = emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

      when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(categorisationInfo))
      when(mockCategorisationService.calculateResult(any(), any(), any()))
        .thenReturn(StandardGoodsNoAssessmentsScenario)
      when(mockGoodsRecordsConnector.getRecord(any())(any()))
        .thenReturn(Future.successful(Some(mockGoodsRecordResponse.copy(category = None))))
      when(mockGoodsRecordsConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val result = autoCategorisationService
        .autoCategoriseRecord(mockGoodsRecordResponse.copy(category = None), userAnswers)(mockDataRequest, hc)
        .futureValue

      result.value shouldBe StandardGoodsNoAssessmentsScenario
    }
  }
}
