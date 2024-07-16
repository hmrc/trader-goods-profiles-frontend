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
import forms.RemoveNiphlFormProvider
import models.TraderProfile
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.RemoveNiphlView
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import pages.HasNiphlUpdatePage
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class RemoveNiphlControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider                       = new RemoveNiphlFormProvider()
  private val form                       = formProvider()
  private val mockSessionRepository      = mock[SessionRepository]
  private val mockTraderProfileConnector = mock[TraderProfileConnector]

  private lazy val removeNiphlRoute = routes.RemoveNiphlController.onPageLoad().url

  "RemoveNiphl Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeNiphlRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNiphlView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when No submitted and not submit" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockTraderProfileConnector = mock[TraderProfileConnector]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockTraderProfileConnector, never()).submitTraderProfile(any(), any())(any())
      }
    }

    "must redirect to the next page when Yes submitted and submit" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNiphlUpdatePage, false).success.value))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockTraderProfileConnector, times(1))
          .submitTraderProfile(eqTo(TraderProfile(testEori, "1", Some("2"), None)), eqTo(testEori))(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveNiphlView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeNiphlRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if Trader Profile cannot be built" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val continueUrl = RedirectUrl(routes.HasNiphlController.onPageLoadUpdate.url)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
      }
    }
  }
}
