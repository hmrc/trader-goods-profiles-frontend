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
import controllers.routes
import controllers.profile.{routes => profileRoutes}
import base.TestConstants.testEori
import config.FrontendAppConfig
import connectors.TraderProfileConnector
import navigation.profile.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.profile.ProfileSetupView

import scala.concurrent.Future

class ProfileSetupControllerSpec extends SpecBase with MockitoSugar {

  "ProfileSetup Controller" - {

    "for a GET" - {

      "must return OK and the correct view" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, profileRoutes.ProfileSetupController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ProfileSetupView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }

      "must redirect to Home page if profile already exists" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, profileRoutes.ProfileSetupController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      val onwardRoute = Call("", "")

      "must redirect to the next page" - {

        "when historic profile data is enabled" in {
          val mockSessionRepository      = mock[SessionRepository]
          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAppConfig              = mock[FrontendAppConfig]

          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
          when(mockTraderProfileConnector.getHistoricProfileData(any())(any())).thenReturn(Future.successful(None))
          when(mockAppConfig.getHistoricProfileEnabled).thenReturn(true)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                bind[FrontendAppConfig].toInstance(mockAppConfig)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, profileRoutes.ProfileSetupController.onSubmit().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockTraderProfileConnector).getHistoricProfileData(eqTo(testEori))(any())
          }
        }

        "when historic profile data is disabled" in {
          val mockSessionRepository      = mock[SessionRepository]
          val mockTraderProfileConnector = mock[TraderProfileConnector]
          val mockAppConfig              = mock[FrontendAppConfig]

          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
          when(mockAppConfig.getHistoricProfileEnabled).thenReturn(false)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                bind[FrontendAppConfig].toInstance(mockAppConfig)
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, profileRoutes.ProfileSetupController.onSubmit().url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            withClue("should not have requested historic data") {
              verify(mockTraderProfileConnector, times(0)).getHistoricProfileData(eqTo(testEori))(any())
            }
          }
        }
      }
    }
  }
}
