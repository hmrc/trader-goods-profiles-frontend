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
import base.TestConstants.testRecordId
import connectors.AccreditationConnector
import models.helper.RequestAdviceJourney
import models.{AdviceRequest, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{EmailSummary, NameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.advice.CyaRequestAdviceView

import scala.concurrent.Future

class CyaRequestAdviceControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaRequestAdviceController" - {
    val expectedPayload = AdviceRequest("eori", "Firstname Lastname", "eori", testRecordId, "test@test.com")

    def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
      rows = Seq(
        NameSummary.row(userAnswers, testRecordId)(messages(app)),
        EmailSummary.row(userAnswers, testRecordId)(messages(app))
      ).flatten
    )

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val userAnswers      = mandatoryAdviceUserAnswers
        val mockAuditService = mock[AuditService]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaRequestAdviceView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list, testRecordId)(request, messages(application)).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditRequestAdvice(any(), any())(any())
          }
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(controllers.advice.routes.AdviceStartController.onPageLoad(testRecordId).url)

        running(application) {
          val request =
            FakeRequest(GET, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(Some(continueUrl))
            .url

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(GET, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "must submit the advice request and redirect to AdviceSuccessController" in {

        val userAnswers = mandatoryAdviceUserAnswers

        val mockConnector = mock[AccreditationConnector]
        when(mockConnector.submitRequestAccreditation(any())(any())).thenReturn(Future.successful(Done))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditRequestAdvice(any(), any())(any))
          .thenReturn(Future.successful(Done))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[AccreditationConnector].toInstance(mockConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.advice.routes.AdviceSuccessController
            .onPageLoad(testRecordId)
            .url

          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService)
              .auditRequestAdvice(
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(
                any()
              )
          }

          withClue("must cleanse the user answers data") {
            verify(sessionRepository).clearData(eqTo(userAnswers.id), eqTo(RequestAdviceJourney))
          }
        }
      }

      "when user answers cannot create a advice request" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector = mock[AccreditationConnector]
          val continueUrl   = RedirectUrl(controllers.advice.routes.AdviceStartController.onPageLoad(testRecordId).url)

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[AccreditationConnector].toInstance(mockConnector))
              .overrides(bind[SessionRepository].toInstance(sessionRepository))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url
            verify(mockConnector, never()).submitRequestAccreditation(any())(any())

            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(emptyUserAnswers.id), eqTo(RequestAdviceJourney))
            }
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatoryAdviceUserAnswers

        val mockConnector = mock[AccreditationConnector]
        when(mockConnector.submitRequestAccreditation(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditRequestAdvice(any(), any())(any))
          .thenReturn(Future.successful(Done))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[AccreditationConnector].toInstance(mockConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("must call the audit connector with the supplied details") {
            verify(mockAuditService)
              .auditRequestAdvice(
                eqTo(AffinityGroup.Individual),
                eqTo(expectedPayload)
              )(
                any()
              )
          }
          withClue("must not cleanse the user answers data when connector fails") {
            verify(sessionRepository, never()).clearData(eqTo(userAnswers.id), eqTo(RequestAdviceJourney))
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.advice.routes.CyaRequestAdviceController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
