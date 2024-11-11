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

package controllers

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import connectors.GoodsRecordConnector
import forms.goodsRecord.RemoveGoodsRecordFormProvider
import models.GoodsRecordsPagination.firstPage
import models.{GoodsProfileLocation, GoodsRecordLocation}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class RemoveGoodsRecordControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveGoodsRecordFormProvider()
  private val form = formProvider()

  private lazy val removeGoodsRecordRoute =
    routes.RemoveGoodsRecordController.onPageLoad(testRecordId, GoodsRecordLocation).url

  private val mockAuditService = mock[AuditService]

  "RemoveGoodsRecord Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockAuditService.auditStartRemoveGoodsRecord(any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveGoodsRecordView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId, GoodsRecordLocation)(
          request,
          messages(application)
        ).toString

        verify(mockAuditService).auditStartRemoveGoodsRecord(any(), any(), any())(any())
      }
    }

    "must return OK and the alternate correct view for a GET with different url" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.RemoveGoodsRecordController.onPageLoad(testRecordId, GoodsProfileLocation).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveGoodsRecordView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId, GoodsProfileLocation)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the goods records list and delete record when Yes is submitted and record is deleted" in {

      val mockConnector = mock[GoodsRecordConnector]

      when(mockConnector.removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[GoodsRecordConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockConnector).removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any())
      }
    }

    "must error when Yes is submitted and record is not there" in {

      val mockConnector = mock[GoodsRecordConnector]

      when(mockAuditService.auditFinishRemoveGoodsRecord(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      when(mockConnector.removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any()))
        .thenReturn(Future.successful(false))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[GoodsRecordConnector].toInstance(mockConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result      = route(application, request).value
        val continueUrl = RedirectUrl(controllers.goodsRecord.routes.GoodsRecordsController.onPageLoad(firstPage).url)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
        verify(mockConnector).removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any())
        verify(mockAuditService).auditFinishRemoveGoodsRecord(any(), any(), any())(any())
      }
    }

    "must redirect to the goods records list when No is submitted" in {
      val mockConnector = mock[GoodsRecordConnector]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockConnector, never()).removeGoodsRecord(any(), any())(any())

      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveGoodsRecordView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, testRecordId, GoodsRecordLocation)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
