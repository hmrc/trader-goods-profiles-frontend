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
import forms.RemoveGoodsRecordFormProvider
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.RemoveGoodsRecordView

import scala.concurrent.Future

class RemoveGoodsRecordControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveGoodsRecordFormProvider()
  private val form = formProvider()

  private lazy val removeGoodsRecordRoute = routes.RemoveGoodsRecordController.onPageLoad(testRecordId).url

  "RemoveGoodsRecord Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveGoodsRecordView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId)(request, messages(application)).toString
      }
    }

    "must redirect to the goods records list and delete record when Yes is submitted" in {

      val mockConnector = mock[GoodsRecordConnector]

      when(mockConnector.removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any()))
        .thenReturn(Future.successful(Done))

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
        verify(mockConnector, times(1)).removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any())
      }
    }

    "must error when Yes is submitted and record is not there" in {

      val mockConnector = mock[GoodsRecordConnector]

      when(mockConnector.removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any()))
        .thenReturn(Future.failed(new RuntimeException("Connector Failed")))

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

        intercept[RuntimeException] {
          await(route(application, request).value)
        }

        withClue("must try to remove the record") {
          verify(mockConnector, times(1)).removeGoodsRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
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

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeGoodsRecordRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveGoodsRecordView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, testRecordId)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeGoodsRecordRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

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
