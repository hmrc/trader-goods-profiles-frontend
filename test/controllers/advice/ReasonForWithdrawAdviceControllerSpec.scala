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

package controllers.advice

import base.SpecBase
import base.TestConstants.{testEori, testRecordId, userAnswersId, withdrawReason}
import connectors.AccreditationConnector
import forms.advice.ReasonForWithdrawAdviceFormProvider
import models.UserAnswers
import models.helper.WithdrawAdviceJourney
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.advice.ReasonForWithdrawAdvicePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.advice.ReasonForWithdrawAdviceView

import scala.concurrent.Future

class ReasonForWithdrawAdviceControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new ReasonForWithdrawAdviceFormProvider()
  private val form         = formProvider()

  private lazy val reasonForWithdrawAdviceRoute =
    controllers.advice.routes.ReasonForWithdrawAdviceController.onPageLoad(testRecordId).url

  "ReasonForWithdrawAdvice Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockAuditService = mock[AuditService]
      val application      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForWithdrawAdviceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReasonForWithdrawAdviceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, testRecordId)(request, messages(application)).toString
        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditRequestAdvice(any(), any())(any())
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId).set(ReasonForWithdrawAdvicePage(testRecordId), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
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
      when(mockConnector.withdrawRequestAccreditation(any(), any())(any())).thenReturn(Future.successful(Done))

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockAuditService = mock[AuditService]
      when(mockAuditService.auditWithdrawAdvice(any(), any(), any(), any())(any))
        .thenReturn(Future.successful(Done))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AccreditationConnector].toInstance(mockConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.advice.routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", withdrawReason))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.advice.routes.WithdrawAdviceSuccessController
          .onPageLoad(testRecordId)
          .url

        withClue("must call the audit connector with the supplied details") {
          verify(mockAuditService, atLeastOnce())
            .auditWithdrawAdvice(
              eqTo(AffinityGroup.Individual),
              eqTo(testEori),
              eqTo(testRecordId),
              eqTo(Some(withdrawReason))
            )(
              any()
            )
          verify(mockConnector, atLeastOnce()).withdrawRequestAccreditation(any(), any())(any())
          verify(mockSessionRepository, atLeastOnce()).clearData(any(), any())
        }
      }
    }

    "must redirect to the next page when empty data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withdrawRequestAccreditation(any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AccreditationConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.advice.routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.advice.routes.WithdrawAdviceSuccessController
          .onPageLoad(testRecordId)
          .url
        verify(mockConnector, atLeastOnce()).withdrawRequestAccreditation(any(), any())(any())
        verify(mockSessionRepository, atLeastOnce()).clearData(any(), any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withdrawRequestAccreditation(any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AccreditationConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val invalidValue = "a" * 513
        val request      =
          FakeRequest(POST, controllers.advice.routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", invalidValue))

        val boundForm = form.bind(Map("value" -> invalidValue))

        val view = application.injector.instanceOf[ReasonForWithdrawAdviceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        contentAsString(result) mustEqual view(
          boundForm,
          testRecordId
        )(request, messages(application)).toString
        verify(mockConnector, never()).withdrawRequestAccreditation(any(), any())(any())
        verify(mockSessionRepository, never()).clearData(any(), any())
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForWithdrawAdviceRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request =
          FakeRequest(POST, reasonForWithdrawAdviceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must let the play error handler deal with connector failure" in {

      val mockSessionRepository = mock[SessionRepository]

      val mockConnector = mock[AccreditationConnector]
      when(mockConnector.withdrawRequestAccreditation(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Connector failed")))

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val mockAuditService = mock[AuditService]
      when(mockAuditService.auditWithdrawAdvice(any(), any(), any(), any())(any))
        .thenReturn(Future.successful(Done))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AccreditationConnector].toInstance(mockConnector),
            bind[AuditService].toInstance(mockAuditService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.advice.routes.ReasonForWithdrawAdviceController.onSubmit(testRecordId).url)
            .withFormUrlEncodedBody(("value", withdrawReason))

        intercept[RuntimeException] {
          await(route(application, request).value)
        }

        withClue("must call the audit connector with the supplied details") {
          verify(mockAuditService, atLeastOnce())
            .auditWithdrawAdvice(
              eqTo(AffinityGroup.Individual),
              eqTo(testEori),
              eqTo(testRecordId),
              eqTo(Some(withdrawReason))
            )(
              any()
            )
          verify(mockConnector).withdrawRequestAccreditation(any(), any())(any())
          verify(mockSessionRepository, never()).clearData(any(), any())
        }
        withClue("must not cleanse the user answers data when connector fails") {
          verify(mockSessionRepository, never()).clearData(eqTo(emptyUserAnswers.id), eqTo(WithdrawAdviceJourney))
        }
      }
    }
  }
}
