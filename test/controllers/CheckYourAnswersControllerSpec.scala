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
import base.TestConstants.testEori
import connectors.RouterConnector
import models.{TraderProfile, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasNiphlPage, HasNirmsPage, UkimsNumberPage}
import play.api.test.FakeRequest
import play.api.inject.bind
import play.api.test.Helpers._
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "Check Your Answers Controller" - {

    "for a GET" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]
          val list = SummaryListViewModel(Seq.empty)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "when user answers can create a valid trader profile" - {

        "must submit the trader profile and redirect to the Home Page" in {

          val userAnswers =
            emptyUserAnswers
              .set(UkimsNumberPage, "1").success.value
              .set(HasNirmsPage, false).success.value
              .set(HasNiphlPage, false).success.value

          val mockConnector = mock[RouterConnector]
          when(mockConnector.submitTraderProfile(any(), any())(any())).thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[RouterConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad.url)

            val result = route(application, request).value

            val expectedPayload = TraderProfile(testEori, "1", None, None)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
            verify(mockConnector, times(1)).submitTraderProfile(eqTo(expectedPayload), eqTo(testEori))(any())
          }
        }
      }

      "when user answers cannot create a trader profile" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector = mock[RouterConnector]

          val application =
            applicationBuilder(userAnswers = Some(UserAnswers("")))
              .overrides(bind[RouterConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(mockConnector, never()).submitTraderProfile(any(), any())(any())
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
