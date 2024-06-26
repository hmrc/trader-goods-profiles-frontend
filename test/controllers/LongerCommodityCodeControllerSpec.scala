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
import connectors.OttConnector
import forms.LongerCommodityCodeFormProvider
import models.{Commodity, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CommodityCodePage, LongerCommodityCodePage}
import play.api.data.FormError
import play.api.http.Status.NOT_FOUND
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.LongerCommodityCodeView

import java.time.Instant
import scala.concurrent.Future

class LongerCommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider   = new LongerCommodityCodeFormProvider()
  private val form           = formProvider()
  private val recordId       = "123"
  private val shortCommodity = "654321"

  private def onwardRoute = Call("GET", "/foo")

  private lazy val longerCommodityCodeRoute = routes.LongerCommodityCodeController.onPageLoad(NormalMode, recordId).url

  "LongerCommodityCode Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = emptyUserAnswers.set(CommodityCodePage, shortCommodity).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, longerCommodityCodeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LongerCommodityCodeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, shortCommodity, recordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, longerCommodityCodeRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect on GET to JourneyRecovery Page if user's commodity code is 10 digits" in {
      val userAnswers = emptyUserAnswers.set(CommodityCodePage, "1234567890").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, longerCommodityCodeRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CommodityCodePage, shortCommodity)
        .success
        .value
        .set(LongerCommodityCodePage, "answer")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, longerCommodityCodeRoute)

        val view = application.injector.instanceOf[LongerCommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, shortCommodity, recordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockOttConnector      = mock[OttConnector]
      val userAnswers           = emptyUserAnswers
        .set(CommodityCodePage, shortCommodity)
        .success
        .value
        .set(LongerCommodityCodePage, "answer")
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any())(any())) thenReturn Future
        .successful(
          Commodity("654321", "Description", Instant.now, None)
        )

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, longerCommodityCodeRoute)
            .withFormUrlEncodedBody(("value", "12"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockOttConnector, times(1)).getCommodityCode(any(), any(), any(), any(), any())(any())
      }
    }

    "must return a Bad Request and errors when invalid is submitted" in {
      val userAnswers = emptyUserAnswers
        .set(CommodityCodePage, shortCommodity)
        .success
        .value
        .set(LongerCommodityCodePage, "answer")
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, longerCommodityCodeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[LongerCommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, recordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when correct data format but wrong data is submitted" in {
      val mockOttConnector = mock[OttConnector]
      val userAnswers      = emptyUserAnswers
        .set(CommodityCodePage, shortCommodity)
        .success
        .value
        .set(LongerCommodityCodePage, "answer")
        .success
        .value

      when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any())(any())) thenReturn Future.failed(
        UpstreamErrorResponse(" ", NOT_FOUND)
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, longerCommodityCodeRoute)
            .withFormUrlEncodedBody(("value", "1234"))

        val boundForm = form.copy(errors = Seq(elems = FormError("value", "longerCommodityCode.error.invalid")))

        val view = application.injector.instanceOf[LongerCommodityCodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, recordId)(
          request,
          messages(application)
        ).toString

        verify(mockOttConnector, times(1)).getCommodityCode(any(), any(), any(), any(), any())(any())
      }

    }
    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, longerCommodityCodeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, longerCommodityCodeRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
