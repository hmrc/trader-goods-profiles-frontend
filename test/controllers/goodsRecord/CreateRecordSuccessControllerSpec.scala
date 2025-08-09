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
import models._
import org.mockito.ArgumentMatchers.{any, anyBoolean, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.{Messages, MessagesImpl}
import play.api.inject
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AutoCategoriseService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.StandardGoodsAsInt
import views.html.goodsRecord.{CreateRecordAutoCategorisationSuccessView, CreateRecordSuccessView}
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.Instant
import scala.concurrent.Future

class CreateRecordSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAutoCategoriseService: AutoCategoriseService = mock[AutoCategoriseService]
  private val mockGoodsRecordConnector: GoodsRecordConnector   = mock[GoodsRecordConnector]

  override def beforeEach(): Unit = super.beforeEach()

  val mockAutoCategorisationView: CreateRecordAutoCategorisationSuccessView =
    mock[CreateRecordAutoCategorisationSuccessView]
  val mockDefaultView: CreateRecordSuccessView                              = mock[CreateRecordSuccessView]

  // Dummy messages instance
  val messages: Messages = MessagesImpl(play.api.i18n.Lang("en"), stubMessagesApi())

  // Instantiate controller with mocks (some params can be null or mocks if unused)
  val controller = new CreateRecordSuccessController(
    messagesApi = stubMessagesApi(),
    identify = null,
    getData = null,
    requireData = null,
    profileAuth = null,
    controllerComponents = stubMessagesControllerComponents(),
    autoCategoriseService = null,
    goodsRecordConnector = null,
    defaultView = mockDefaultView,
    autoCategorisationView = mockAutoCategorisationView
  )

  // A fake implicit request for calling renderView
  implicit val request: Request[_] = FakeRequest()

  override def afterEach(): Unit = {
    super.afterEach()
    reset(mockAutoCategoriseService, mockGoodsRecordConnector)
  }

  private def applicationWithMocks = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(
      inject.bind[AutoCategoriseService].toInstance(mockAutoCategoriseService),
      inject.bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
    )
    .build()

  "CreateRecordSuccess Controller" - {

    "must return BAD_REQUEST when recordId is empty" in {
      val application = applicationWithMocks

      running(application) {
        val controller = application.injector.instanceOf[CreateRecordSuccessController]
        val result     = controller.onPageLoad("").apply(FakeRequest(GET, "/"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) must include("Invalid record ID")
      }
    }

    "must return OK and the correct view for a GET" in {
      val record = GetGoodsRecordResponse(
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
      when(mockAutoCategoriseService.autoCategoriseRecord(any[String](), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val application = applicationWithMocks

      running(application) {
        val request =
          FakeRequest(GET, controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad("test").url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view("test", None)(request, messages(application)).toString
      }
    }

    "must return OK and correct view when isImmiReady is true" in {
      val record = GetGoodsRecordResponse(
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
        mockAutoCategoriseService.autoCategoriseRecord(eqTo(TestConstants.testRecordId), any[UserAnswers])(
          any[DataRequest[_]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(StandardGoodsScenario)))
      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationWithMocks

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad(TestConstants.testRecordId).url
        )
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]
        val tagText = messages(application)("declarableStatus.immiReady")

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TestConstants.testRecordId, true, tagText)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and correct view when isImmiReady is false" in {
      val record = GetGoodsRecordResponse(
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
        mockAutoCategoriseService.autoCategoriseRecord(eqTo(TestConstants.testRecordId), any[UserAnswers])(
          any[DataRequest[_]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(StandardGoodsScenario)))

      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationWithMocks

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad(TestConstants.testRecordId).url
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

    "must return OK and default view when declarable is NotReadyForUse" in {
      val record = GetGoodsRecordResponse(
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
        declarable = DeclarableStatus.NotReadyForUse,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      when(
        mockAutoCategoriseService.autoCategoriseRecord(eqTo(TestConstants.testRecordId), any[UserAnswers])(
          any[DataRequest[_]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(StandardGoodsScenario)))

      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationWithMocks

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad(TestConstants.testRecordId).url
        )
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TestConstants.testRecordId, Some(StandardGoodsScenario))(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and default view when scenario is None" in {
      val record = GetGoodsRecordResponse(
        recordId = TestConstants.testRecordId,
        eori = TestConstants.testEori,
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

      when(
        mockAutoCategoriseService.autoCategoriseRecord(eqTo(TestConstants.testRecordId), any[UserAnswers])(
          any[DataRequest[_]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(None))

      when(mockGoodsRecordConnector.getRecord(eqTo(TestConstants.testRecordId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(record))

      val application = applicationWithMocks

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad(TestConstants.testRecordId).url
        )
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[CreateRecordSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TestConstants.testRecordId, None)(
          request,
          messages(application)
        ).toString
      }
    }
  }

  "renderView" - {

    "render autoCategorisationView with isImmiReady = true when declarable is ImmiReady" in {
      val record = GetGoodsRecordResponse(
        recordId = "id",
        eori = "eori",
        actorId = "actor",
        traderRef = "ref",
        comcode = "code",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "desc",
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

      when(mockAutoCategorisationView.apply(any(), anyBoolean(), any())(any(), any()))
        .thenReturn(Html("AutoCategorisationViewRendered"))

      val result: Result = controller.renderView("test", Some(StandardGoodsScenario), record)(request, messages)

      status(Future.successful(result)) mustBe OK
      contentAsString(Future.successful(result)) must include("AutoCategorisationViewRendered")

      verify(mockAutoCategorisationView).apply("test", true, messages(record.declarable.messageKey))(request, messages)
    }

    "render autoCategorisationView with isImmiReady = false when declarable is NotReadyForImmi" in {
      val record = GetGoodsRecordResponse(
        recordId = "id",
        eori = "eori",
        actorId = "actor",
        traderRef = "ref",
        comcode = "code",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "desc",
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
        declarable = DeclarableStatus.NotReadyForImmi,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )
      when(mockAutoCategorisationView.apply(any(), anyBoolean(), any())(any(), any()))
        .thenReturn(Html("AutoCategorisationViewRendered"))

      val result: Result = controller.renderView("test", Some(StandardGoodsScenario), record)(request, messages)

      status(Future.successful(result)) mustBe OK
      contentAsString(Future.successful(result)) must include("AutoCategorisationViewRendered")

      verify(mockAutoCategorisationView).apply(
        eqTo("test"),
        eqTo(false),
        eqTo("declarableStatus.notReadyForImmi")
      )(eqTo(request), eqTo(messages))
    }

    "render defaultView when declarable is NotReadyForUse" in {
      val record = GetGoodsRecordResponse(
        recordId = "id",
        eori = "eori",
        actorId = "actor",
        traderRef = "ref",
        comcode = "code",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "desc",
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
        declarable = DeclarableStatus.NotReadyForUse,
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      // Stub the default view (not autoCategorisationView)
      when(mockDefaultView.apply(any(), any())(any(), any()))
        .thenReturn(Html("DefaultViewRendered"))

      val result = controller.renderView("id", Some(StandardGoodsScenario), record)(request, messages)

      status(Future.successful(result)) mustBe OK
      contentAsString(Future.successful(result)) must include("DefaultViewRendered")

      verify(mockDefaultView).apply(
        eqTo("id"),
        eqTo(Some(StandardGoodsScenario))
      )(eqTo(request), eqTo(messages))
    }

    "render defaultView when scenario is None" in {
      val record = GetGoodsRecordResponse(
        recordId = "id",
        eori = "eori",
        actorId = "actor",
        traderRef = "ref",
        comcode = "code",
        adviceStatus = AdviceStatus.NotRequested,
        goodsDescription = "desc",
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
        declarable = DeclarableStatus.ImmiReady, // Still renders defaultView if scenario is None
        ukimsNumber = None,
        nirmsNumber = None,
        niphlNumber = None,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now()
      )

      // Return Html, not Result!
      when(mockDefaultView.apply(any(), any())(any(), any()))
        .thenReturn(Html("DefaultViewRendered"))

      val result: Result = controller.renderView("id", None, record)(request, messages)

      status(Future.successful(result)) mustBe OK
      contentAsString(Future.successful(result)) must include("DefaultViewRendered")

      verify(mockDefaultView).apply(
        eqTo("id"),
        eqTo(None)
      )(eqTo(request), eqTo(messages))
    }
  }

}
