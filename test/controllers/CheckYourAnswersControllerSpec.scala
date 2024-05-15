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
import pages.{HasNiphlPage, HasNirmsPage, NiphlNumberPage, NirmsNumberPage, UkimsNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{HasNiphlSummary, HasNirmsSummary, NiphlNumberSummary, NirmsNumberSummary, UkimsNumberSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "Check Your Answers Controller" - {

    def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
      rows = Seq(
        UkimsNumberSummary.row(userAnswers)(messages(app)),
        HasNirmsSummary.row(userAnswers)(messages(app)),
        NirmsNumberSummary.row(userAnswers)(messages(app)),
        HasNiphlSummary.row(userAnswers)(messages(app)),
        NiphlNumberSummary.row(userAnswers)(messages(app))
      ).flatten
    )

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(UkimsNumberPage, "1").success.value
          .set(HasNirmsPage, false).success.value
          .set(HasNiphlPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view with all data (including optional)" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(UkimsNumberPage, "1").success.value
          .set(HasNirmsPage, true).success.value
          .set(NirmsNumberPage, "2").success.value
          .set(HasNiphlPage, true).success.value
          .set(NiphlNumberPage, "3").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

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
          val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

          val application =
            applicationBuilder(userAnswers = Some(UserAnswers("")))
              .overrides(bind[RouterConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
            verify(mockConnector, never()).submitTraderProfile(any(), any())(any())
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers =
          emptyUserAnswers
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, false).success.value
            .set(HasNiphlPage, false).success.value

        val mockConnector = mock[RouterConnector]
        when(mockConnector.submitTraderProfile(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[RouterConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
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
