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

package controllers.profile

import base.SpecBase
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import forms.RemoveNiphlFormProvider
import models.{TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasNiphlUpdatePage, NiphlNumberUpdatePage, RemoveNiphlPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository

import scala.concurrent.Future

class RemoveNiphlControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new RemoveNiphlFormProvider()
  private val form = formProvider()

  private lazy val removeNiphlRoute = routes.RemoveNiphlController.onPageLoad().url

  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "RemoveNiphl Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNiphlRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNiphlView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(RemoveNiphlPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNiphlRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNiphlView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true))(request, messages(application)).toString
      }
    }

    "must redirect to the next page when No submitted and save the answers" in {
      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", None, Some("SN12345"), eoriChanged = false))
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
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

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNiphlPage).get mustBe false
        }

        withClue("must have saved the niphl number as future items will depend on it") {
          finalUserAnswers.get(NiphlNumberUpdatePage).get mustBe "SN12345"
        }

      }
    }

    "must redirect to the next page when No submitted and not overwrite the existing niphl value" in {
      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", None, Some("SN12345"), eoriChanged = false))
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(NiphlNumberUpdatePage, "SN12346").success.value))
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

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNiphlPage).get mustBe false
        }

        withClue("must not overwrite the user entered niphl") {
          finalUserAnswers.get(NiphlNumberUpdatePage).get mustBe "SN12346"
        }

      }
    }

    "must redirect to the next page when Yes submitted" in {

      val mockSessionRepository                               = mock[SessionRepository]
      val finalUserAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockSessionRepository.set(finalUserAnswersCaptor.capture())).thenReturn(Future.successful(true))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
        Future.successful(TraderProfile(testEori, "1", None, Some("933844"), eoriChanged = false))
      )

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

        val finalUserAnswers = finalUserAnswersCaptor.getValue

        withClue("must have saved the answer") {
          finalUserAnswers.get(RemoveNiphlPage).get mustBe true
        }

        withClue("must not have saved the nirms number as not needed") {
          finalUserAnswers.get(NiphlNumberUpdatePage) mustBe None
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

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

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNiphlRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNiphlRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
