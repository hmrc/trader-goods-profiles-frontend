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
import base.TestConstants.{testEori, userAnswersId}
import connectors.OttConnector
import forms.CommodityCodeFormProvider
import models.GoodsRecord.newRecordId
import models.{Commodity, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.FormError
import play.api.http.Status.NOT_FOUND
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.CommodityCodeView

import java.time.Instant
import scala.concurrent.Future

class CommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new CommodityCodeFormProvider()
  private val form = formProvider()

  private lazy val commodityCodeRoute = routes.CommodityCodeController.onPageLoad(NormalMode, newRecordId).url

  "CommodityCode Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, commodityCodeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CommodityCodeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, newRecordId)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(CommodityCodePage(newRecordId), "654321").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, commodityCodeRoute)

        val view = application.injector.instanceOf[CommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("654321"), NormalMode, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      val mockOttConnector = mock[OttConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any())(any())) thenReturn Future
        .successful(
          Commodity("654321", List("Class level1 desc", "Class level2 desc", "Class level3 desc"), Instant.now, None)
        )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "654321"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockOttConnector, times(1)).getCommodityCode(eqTo("654321"), eqTo(testEori), any(), any(), any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when incorrect data format is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "abc"))

        val boundForm = form.bind(Map("value" -> "abc"))

        val view = application.injector.instanceOf[CommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when correct data format but wrong data is submitted" in {

      val mockOttConnector = mock[OttConnector]

      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any())(any())) thenReturn Future.failed(
        UpstreamErrorResponse(" ", NOT_FOUND)
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "654321"))

        val boundForm = form.copy(errors = Seq(elems = FormError("value", "Enter a real commodity code")))

        val view = application.injector.instanceOf[CommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, newRecordId)(
          request,
          messages(application)
        ).toString

        verify(mockOttConnector, times(1)).getCommodityCode(eqTo("654321"), eqTo(testEori), any(), any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, commodityCodeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, commodityCodeRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
