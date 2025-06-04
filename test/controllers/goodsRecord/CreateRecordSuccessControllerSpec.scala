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

package controllers.goodsRecord

import base.{SpecBase, TestConstants}
import connectors.GoodsRecordConnector
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import models.{AdviceStatus, DeclarableStatus, StandardGoodsScenario, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AutoCategoriseService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.StandardGoodsAsInt
import views.html.goodsRecord.{CreateRecordAutoCategorisationSuccessView, CreateRecordSuccessView}

import java.time.Instant
import scala.concurrent.Future

class CreateRecordSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAutoCategoriseService: AutoCategoriseService = mock[AutoCategoriseService]
  private val mockGoodsRecordConnector                         = mock[GoodsRecordConnector]

  override def beforeEach(): Unit =
    super.beforeEach()

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockAutoCategoriseService)
    reset(mockGoodsRecordConnector)
  }

  "CreateRecordSuccess Controller" - {
    "must return OK and the correct view for a GET" in {
      val record: GetGoodsRecordResponse = GetGoodsRecordResponse(
        recordId = "test",
        eori = "eori",
        actorId = "actor123",
        traderRef = "ref123",
        comcode = "123456",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "Test Goods",
        countryOfOrigin = "GB",
        category = None,
        assessments = None,
        supplementaryUnit = None,
        measurementUnit = None,
        comcodeEffectiveFromDate = Instant.now(),
        comcodeEffectiveToDate = None,
        version = 1,
        active = true,
        toReview = false,
        reviewReason = None,
        declarable = DeclarableStatus.NotReadyForUse,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      when(mockGoodsRecordConnector.getRecord(eqTo("test"))(any[HeaderCarrier])).thenReturn(Future.successful(record))

      when(mockAutoCategoriseService.autoCategoriseRecord(any[String](), any())(any(), any())) thenReturn Future
        .successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService))
        .overrides(inject.bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad("test").url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view("test", None)(request, messages(application)).toString
      }
    }

    "must return OK and correct view when isImmiReady is true" in {
      val record: GetGoodsRecordResponse = GetGoodsRecordResponse(
        recordId = TestConstants.testRecordId,
        eori = TestConstants.testEori,
        actorId = "actor123",
        traderRef = "ref123",
        comcode = "123456",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "Test Goods",
        countryOfOrigin = "GB",
        category = Some(StandardGoodsAsInt),
        assessments = None,
        supplementaryUnit = None,
        measurementUnit = None,
        comcodeEffectiveFromDate = Instant.now(),
        comcodeEffectiveToDate = None,
        version = 1,
        active = true,
        toReview = false,
        reviewReason = None,
        declarable = DeclarableStatus.ImmiReady,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      when(
        mockAutoCategoriseService.autoCategoriseRecord(
          eqTo(TestConstants.testRecordId),
          any[UserAnswers]
        )(any[DataRequest[_]], any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(StandardGoodsScenario)))

      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
          inject.bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        ).build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.goodsRecord.routes.CreateRecordSuccessController
            .onPageLoad(TestConstants.testRecordId).url
        )

        val result = route(application, request).value
        val view   = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]
        val tagText = messages(application)("declarableStatus.immiReady")

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TestConstants.testRecordId, true, tagText)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and correct view when isImmiReady is false" in {
      val record: GetGoodsRecordResponse = GetGoodsRecordResponse(
        recordId = TestConstants.testRecordId,
        eori = TestConstants.testEori,
        actorId = "actor123",
        traderRef = "ref123",
        comcode = "123456",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "Test Goods",
        countryOfOrigin = "GB",
        category = Some(StandardGoodsAsInt),
        assessments = None,
        supplementaryUnit = None,
        measurementUnit = None,
        comcodeEffectiveFromDate = Instant.now(),
        comcodeEffectiveToDate = None,
        version = 1,
        active = true,
        toReview = true,
        reviewReason = None,
        declarable = DeclarableStatus.NotReadyForImmi,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      when(
        mockAutoCategoriseService.autoCategoriseRecord(
          eqTo(TestConstants.testRecordId),
          any[UserAnswers]
        )(any[DataRequest[_]], any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(StandardGoodsScenario)))

      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
          inject.bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        ).build()

      running(application) {
        val request = FakeRequest(
          GET,controllers.goodsRecord.routes.CreateRecordSuccessController
            .onPageLoad(TestConstants.testRecordId).url
        )

        val result  = route(application, request).value
        val view    = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]
        val tagText = messages(application)("declarableStatus.notReadyForImmi")

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TestConstants.testRecordId, false, tagText)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}