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
import connectors.{AccreditationConnector, TraderProfileConnector}
import forms.ReasonForWithdrawAdviceFormProvider
import models.UserAnswers
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.ReasonForWithdrawAdvicePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.ReasonForWithdrawAdviceView

import scala.concurrent.Future

class ReasonForWithdrawAdviceControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new ReasonForWithdrawAdviceFormProvider()
  private val form         = formProvider()

  private lazy val reasonForWithdrawAdviceRoute = routes.ReasonForWithdrawAdviceController.onPageLoad(testRecordId).url

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "ReasonForWithdrawAdvice Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForWithdrawAdviceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReasonForWithdrawAdviceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId).set(ReasonForWithdrawAdvicePage(testRecordId), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForWithdrawAdviceRoute)

        val view = application.injector.instanceOf[ReasonForWithdrawAdviceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), testRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withDrawRequestAccreditation(any(), any(), any())(any())).thenReturn(Future.successful(Done))

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AccreditationConnector].toInstance(mockConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", "test"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.WithdrawAdviceSuccessController.onPageLoad(testRecordId).url
      }
    }

    "must redirect to the next page when empty data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withDrawRequestAccreditation(any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AccreditationConnector].toInstance(mockConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.WithdrawAdviceSuccessController.onPageLoad(testRecordId).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withDrawRequestAccreditation(any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AccreditationConnector].toInstance(mockConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val invalidValue = "a" * 513
        val request      =
          FakeRequest(POST, routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", invalidValue))

        val boundForm = form.bind(Map("value" -> invalidValue))

        val view = application.injector.instanceOf[ReasonForWithdrawAdviceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual view(
          boundForm,
          testRecordId
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForWithdrawAdviceRoute)

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
          FakeRequest(POST, reasonForWithdrawAdviceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
