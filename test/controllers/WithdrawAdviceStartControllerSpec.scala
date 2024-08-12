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
import base.TestConstants.{testRecordId, userAnswersId}
import connectors.TraderProfileConnector
import forms.WithdrawAdviceStartFormProvider
import models.UserAnswers
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.WithdrawAdviceStartPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.WithdrawAdviceStartView

import scala.concurrent.Future

class WithdrawAdviceStartControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new WithdrawAdviceStartFormProvider()
  private val form         = formProvider()

  private lazy val withdrawAdviceStartRoute              = routes.WithdrawAdviceStartController.onPageLoad(testRecordId).url
  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "WithdrawAdviceStart Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, withdrawAdviceStartRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WithdrawAdviceStartView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WithdrawAdviceStartPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, withdrawAdviceStartRoute)

        val view = application.injector.instanceOf[WithdrawAdviceStartView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), testRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

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
          FakeRequest(POST, withdrawAdviceStartRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, withdrawAdviceStartRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WithdrawAdviceStartView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, testRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, withdrawAdviceStartRoute)

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
          FakeRequest(POST, withdrawAdviceStartRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
