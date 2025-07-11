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

package controllers.profile.ukims

import base.SpecBase
import base.TestConstants.userAnswersId
import connectors.TraderProfileConnector
import controllers.profile.ukims.routes.*
import forms.profile.ukims.UseExistingUkimsNumberFormProvider
import models.UserAnswers
import navigation.{FakeProfileNavigator, ProfileNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.ukims.UkimsNumberPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.profile.UseExistingUkimsNumberView

import scala.concurrent.Future

class UseExistingUkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new UseExistingUkimsNumberFormProvider()
  private val form         = formProvider()

  private val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  private val mockSessionRepository: SessionRepository           = mock[SessionRepository]

  private lazy val onPageLoadRoute = UseExistingUkimsNumberController.onPageLoad().url
  private lazy val onSubmitRoute   = UseExistingUkimsNumberController.onSubmit().url

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  private val ukimsNumber = "UKIMS123"

  private val userAnswersWithUkimsNumber = UserAnswers(userAnswersId).set(UkimsNumberPage, ukimsNumber).success.value

  "onPageLoad" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkimsNumber))
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UseExistingUkimsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          UseExistingUkimsNumberController.onSubmit(),
          ukimsNumber
        )(request, messages(application)).toString
      }
    }

    "must redirect to the journey recovery page when user answers doesn't have a ukims number pre-populated" in {
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockTraderProfileConnector, times(2)).checkTraderProfile(any())(any())
      }
    }
  }

  "onSubmit" - {

    "must return OK and and navigate to the next page" in {
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkimsNumber))
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkimsNumber))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[UseExistingUkimsNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          UseExistingUkimsNumberController.onSubmit(),
          ukimsNumber
        )(request, messages(application)).toString
      }
    }

    "must throw exception to error handler if session repository fails" in {
      when(mockSessionRepository.set(any())) thenReturn Future.failed(new RuntimeException("failed"))
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkimsNumber))
        .overrides(
          bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, onSubmitRoute).withFormUrlEncodedBody(("value", "true"))

        intercept[RuntimeException] {
          await(route(application, request).value)
          verify(mockSessionRepository).set(any())
        }
      }
    }
  }
}
