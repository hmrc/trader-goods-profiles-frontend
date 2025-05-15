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

import base.SpecBase
import base.TestConstants.testRecordId
import connectors.GoodsRecordConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AutoCategoriseService
import views.html.goodsRecord.{CreateRecordAutoCategorisationSuccessView, CreateRecordSuccessView}
import models.router.responses.GetGoodsRecordResponse
import models.{AdviceStatus, DeclarableStatus}

import java.time.Instant
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateRecordSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAutoCategoriseService: AutoCategoriseService = mock[AutoCategoriseService]
  private val mockGoodsRecordConnector                         = mock[GoodsRecordConnector]

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

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockGoodsRecordConnector.getRecord(eqTo("test"))(any[HeaderCarrier])).thenReturn(Future.successful(record))

  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockAutoCategoriseService)
    reset(mockGoodsRecordConnector)
  }

  "CreateRecordSuccess Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockAutoCategoriseService.autoCategoriseRecord(any[String](), any())(any(), any())) thenReturn Future
        .successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService))
        .overrides(inject.bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad("test").url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view("test", None)(request, messages(application)).toString
      }
    }

    "must return OK and correct view when isImmiReady is true" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordAutoCategorisationSuccessController
            .onPageLoad(testRecordId, true)
            .url
        )

        val result = route(application, request).value
        val view   = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId, true, "testString")(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and correct view when isImmiReady is false" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordAutoCategorisationSuccessController
            .onPageLoad(testRecordId, false)
            .url
        )

        val result = route(application, request).value
        val view   = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId, false, "testString")(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
