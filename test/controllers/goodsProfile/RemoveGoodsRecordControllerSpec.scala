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

package controllers.goodsProfile

import base.SpecBase
import base.TestConstants.testRecordId
import connectors.GoodsRecordConnector
import forms.goodsProfile.RemoveGoodsRecordFormProvider
import models.DeclarableStatus.ImmiReady
import models.GoodsRecordsPagination.firstPage
import models.router.responses.GetGoodsRecordResponse
import models.{AdviceStatus, GoodsProfileLocation, GoodsRecordLocation}
import navigation.{FakeGoodsProfileNavigator, GoodsProfileNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.ProductReferenceUpdatePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.goodsProfile.RemoveGoodsRecordView

import java.time.Instant
import scala.concurrent.Future

class RemoveGoodsRecordControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveGoodsRecordFormProvider()
  private val form = formProvider()

  private lazy val removeGoodsRecordRoute =
    controllers.goodsProfile.routes.RemoveGoodsRecordController.onPageLoad(testRecordId, GoodsRecordLocation).url
  private val userAnswersWithProductRef   =
    emptyUserAnswers.set(ProductReferenceUpdatePage(testRecordId), "productRef").success.value

  private val testRecord = GetGoodsRecordResponse(
    recordId = testRecordId,
    eori = "GB123456789000",
    actorId = "actor-123",
    traderRef = "productRef",
    comcode = "1234567890",
    adviceStatus = AdviceStatus.NotRequested,
    goodsDescription = "Sample goods",
    countryOfOrigin = "GB",
    category = Some(1),
    assessments = None,
    supplementaryUnit = None,
    measurementUnit = None,
    comcodeEffectiveFromDate = Instant.now(),
    comcodeEffectiveToDate = None,
    version = 1,
    active = true,
    toReview = false,
    reviewReason = None,
    declarable = ImmiReady,
    ukimsNumber = None,
    nirmsNumber = None,
    niphlNumber = None,
    createdDateTime = Instant.now(),
    updatedDateTime = Instant.now()
  )

  private val mockAuditService = mock[AuditService]
  private val mockConnector    = mock[GoodsRecordConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService, mockConnector)
  }

  "RemoveGoodsRecord Controller" - {
    "must return OK and the correct view for a GET" in {
      when(mockAuditService.auditStartRemoveGoodsRecord(any(), any(), any())(any())).thenReturn(Future.successful(Done))
      when(mockConnector.getRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(testRecord))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithProductRef))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockConnector),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[RemoveGoodsRecordView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, testRecordId, GoodsRecordLocation, "productRef")(
          request,
          messages(application)
        ).toString
        verify(mockAuditService).auditStartRemoveGoodsRecord(any(), any(), any())(any())
      }
    }

    "must return OK and the alternate correct view for a GET with different url" in {
      when(mockAuditService.auditStartRemoveGoodsRecord(any(), any(), any())(any())).thenReturn(Future.successful(Done))
      when(mockConnector.getRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(testRecord))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithProductRef))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockConnector),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.goodsProfile.routes.RemoveGoodsRecordController.onPageLoad(testRecordId, GoodsProfileLocation).url
        )
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[RemoveGoodsRecordView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId, GoodsProfileLocation, "productRef")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the goods profile list and delete record when Yes is submitted and record is deleted" in {
      when(mockConnector.getRecord(eqTo(testRecordId))(any()))
        .thenReturn(Future.successful(testRecord))
      when(mockConnector.removeGoodsRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockConnector),
          bind[AuditService].toInstance(mockAuditService),
          bind[GoodsProfileNavigator].toInstance(new FakeGoodsProfileNavigator(onwardRoute))
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, removeGoodsRecordRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
        verify(mockConnector).removeGoodsRecord(eqTo(testRecordId))(any())
      }
    }

    "must error when Yes is submitted and record is not there" in {
      when(mockAuditService.auditStartRemoveGoodsRecord(any(), any(), any())(any())).thenReturn(Future.successful(Done))
      when(mockAuditService.auditFinishRemoveGoodsRecord(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockConnector.removeGoodsRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(false))
      when(mockConnector.getRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(testRecord))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithProductRef))
        .overrides(
          bind[GoodsProfileNavigator].toInstance(new FakeGoodsProfileNavigator(onwardRoute)),
          bind[GoodsRecordConnector].toInstance(mockConnector),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      when(mockConnector.removeGoodsRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(false))

      running(application) {
        val request = FakeRequest(POST, removeGoodsRecordRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.RecordNotFoundController.onPageLoad().url

        verify(mockConnector).removeGoodsRecord(eqTo(testRecordId))(any())
        verify(mockAuditService, times(1)).auditFinishRemoveGoodsRecord(any(), any(), any())(any())
      }
    }

    "must redirect to the goods profile list when No is submitted and location is GoodsProfileLocation" in {
      val mockConnector = mock[GoodsRecordConnector]

      when(mockConnector.getRecord(eqTo(testRecordId))(any())).thenReturn(Future.successful(testRecord))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsProfileNavigator].toInstance(new FakeGoodsProfileNavigator(onwardRoute)),
          bind[GoodsRecordConnector].toInstance(mockConnector)
        )
        .build()

      val location = GoodsProfileLocation
      val request  = FakeRequest(POST, routes.RemoveGoodsRecordController.onSubmit(testRecordId, location).url)
        .withFormUrlEncodedBody(("value", "false"))

      running(application) {
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.goodsProfile.routes.GoodsRecordsController
          .onPageLoad(firstPage)
          .url
        verify(mockConnector, never()).removeGoodsRecord(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockConnector = mock[GoodsRecordConnector]

      when(mockConnector.getRecord(eqTo(testRecordId))(any()))
        .thenReturn(Future.successful(testRecord))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request   = FakeRequest(POST, removeGoodsRecordRoute).withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[RemoveGoodsRecordView]
        val result    = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual view(boundForm, testRecordId, GoodsRecordLocation, "productRef")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, removeGoodsRecordRoute).withFormUrlEncodedBody(("value", "true"))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
