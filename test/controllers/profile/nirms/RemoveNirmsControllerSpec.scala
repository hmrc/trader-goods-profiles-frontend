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

package controllers.profile.nirms

import base.SpecBase
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import controllers.profile.nirms.routes._
import forms.profile.nirms.RemoveNirmsFormProvider
import models.{TraderProfile, UserAnswers}
import navigation.{FakeProfileNavigator, ProfileNavigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.nirms.{HasNirmsUpdatePage, NirmsNumberUpdatePage, RemoveNirmsPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.profile.RemoveNirmsView

import scala.concurrent.Future

class RemoveNirmsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveNirmsFormProvider()
  private val form = formProvider()

  private lazy val removeNirmsRoute = RemoveNirmsController.onPageLoad().url

  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "RemoveNirms Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNirmsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNirmsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNirmsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNirmsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true))(request, messages(application)).toString
      }
    }

    "must redirect to the next page when No submitted and save the answers" in {
      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", Some("RMS-GB-848211"), None, eoriChanged = false))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNirmsPage).get mustBe false
        }

        withClue("must have saved the nirms number as future items will depend on it") {
          finalUserAnswers.get(NirmsNumberUpdatePage).get mustBe "RMS-GB-848211"
        }

      }
    }

    "must redirect to the next page when No submitted and not overwrite the existing nirms value" in {
      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", Some("RMS-GB-848211"), None, eoriChanged = false))
      )

      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.set(NirmsNumberUpdatePage, "RMS-XI-111333").success.value)
      )
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNirmsPage).get mustBe false
        }

        withClue("must not overwrite the user entered nirms") {
          finalUserAnswers.get(NirmsNumberUpdatePage).get mustBe "RMS-XI-111333"
        }

      }
    }

    "must redirect to the next page when Yes submitted and save the answers" in {
      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", Some("RMS-GB-848211"), None, eoriChanged = false))
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNirmsUpdatePage, false).success.value))
          .overrides(
            bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNirmsPage).get mustBe true
        }

        withClue("must not have saved the nirms number as not needed") {
          finalUserAnswers.get(NirmsNumberUpdatePage) mustBe None
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveNirmsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNirmsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
