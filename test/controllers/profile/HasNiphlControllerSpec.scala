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
import controllers.routes
import forms.HasNiphlFormProvider
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.{HasNiphlPage, HasNiphlUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.profile.HasNiphlView

import scala.concurrent.Future

class HasNiphlControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasNiphlFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  private lazy val hasNiphlRouteCreate = controllers.profile.routes.HasNiphlController.onPageLoadCreate(NormalMode).url

  private lazy val hasNiphlRouteUpdate = controllers.profile.routes.HasNiphlController.onPageLoadUpdate(NormalMode).url

  "HasNiphlController" - {

    ".create" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasNiphlRouteCreate)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasNiphlView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            controllers.profile.routes.HasNiphlController.onSubmitCreate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasNiphlRouteCreate)

          val view = application.injector.instanceOf[HasNiphlView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            controllers.profile.routes.HasNiphlController.onSubmitCreate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteCreate)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteCreate)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasNiphlView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            controllers.profile.routes.HasNiphlController.onSubmitCreate(NormalMode)
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
          val request = FakeRequest(GET, hasNiphlRouteCreate)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Homepage for a GET if profile already exists" in {

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasNiphlRouteCreate)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteCreate)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".update" - {

      "must return OK and the correct view for a GET with saved answers" in {

        val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

        when(mockTraderProfileConnector.getTraderProfile(eqTo(testEori))(any())) thenReturn Future.successful(
          traderProfile
        )

        when(mockSessionRepository.set(any())) thenReturn Future.successful(
          true
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasNiphlRouteUpdate)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasNiphlView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            controllers.profile.routes.HasNiphlController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, hasNiphlRouteUpdate)

          val view = application.injector.instanceOf[HasNiphlView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(true),
            controllers.profile.routes.HasNiphlController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val traderProfile = TraderProfile(testEori, "1", Some("2"), None, eoriChanged = false)

        when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteUpdate)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect to ProfilePage when no changes made" in {

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), false)

        when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteUpdate)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, hasNiphlRouteUpdate)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasNiphlView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm,
            controllers.profile.routes.HasNiphlController.onSubmitUpdate(NormalMode)
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
          val request = FakeRequest(GET, hasNiphlRouteUpdate)

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
            FakeRequest(POST, hasNiphlRouteUpdate)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
