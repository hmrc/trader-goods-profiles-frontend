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
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import controllers.profile.ukims.routes._
import controllers.routes
import forms.profile.ukims.UkimsNumberFormProvider
import models.{CheckMode, NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeProfileNavigator, ProfileNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.ukims.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import views.html.profile.UkimsNumberView

import scala.concurrent.Future

class UkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new UkimsNumberFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  "UkimsNumberController" - {

    ".create" - {

      val ukimsNumberRoute = UkimsNumberController.onPageLoadCreate(NormalMode).url

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UkimsNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            UkimsNumberController.onSubmitCreate(NormalMode),
            isCreateJourney = true
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(UkimsNumberPage, "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("answer"),
            UkimsNumberController.onSubmitCreate(NormalMode),
            isCreateJourney = true
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockTraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "XIUKIM47699357400020231115081800"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val mockTraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            UkimsNumberController.onSubmitCreate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Homepage for a GET if profile already exists" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val mockTraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".update" - {

      val ukimsNumberRoute = UkimsNumberController.onPageLoadUpdate(NormalMode).url

      "must return OK and the correct view for a GET with all trader profile complete" in {

        val traderProfile    = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
        val mockAuditService = mock[AuditService]

        when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
          traderProfile
        )

        when(mockSessionRepository.set(any())) thenReturn Future.successful(
          true
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UkimsNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("1"),
            UkimsNumberController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return OK and the correct view for a GET with all trader profile complete if the user is returning from the CYA page and UKIMS number should be filled in" in {

        val ukimsNumberCheckRoute = UkimsNumberController.onPageLoadUpdate(CheckMode).url
        val newUkims              = "newUkims"
        val mockAuditService      = mock[AuditService]

        val userAnswers = emptyUserAnswers.set(UkimsNumberUpdatePage, newUkims).success.value

        when(mockSessionRepository.set(any())) thenReturn Future.successful(
          true
        )

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberCheckRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UkimsNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(newUkims),
            UkimsNumberController.onSubmitUpdate(CheckMode)
          )(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit or get profile") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must redirect for a POST" in {

        val answer = "XIUKIM47699357400020231115081800"

        val userAnswers = emptyUserAnswers
          .set(UkimsNumberUpdatePage, answer)
          .success
          .value

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", answer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository, times(3)).set(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            UkimsNumberController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
