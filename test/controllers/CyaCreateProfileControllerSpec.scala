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
import connectors.TraderProfileConnector
import models.{TraderProfile, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers._
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCreateProfileView

import scala.concurrent.Future

class CyaCreateProfileControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCreateProfileController" - {

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

        val userAnswers = mandatoryProfileUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateProfileController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateProfileView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view with all data (including optional)" in {

        val userAnswers = fullProfileUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateProfileController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateProfileView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateProfileController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateProfileController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "when user answers can create a valid trader profile" - {

        "must submit the trader profile and redirect to the Home Page" in {

          val userAnswers = mandatoryProfileUserAnswers

          val mockConnector = mock[TraderProfileConnector]
          when(mockConnector.submitTraderProfile(any(), any())(any())).thenReturn(Future.successful(Done))

          val mockAuditService = mock[AuditService]
          when(mockAuditService.auditProfileSetUp(any(), any())(any())).thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCreateProfileController.onPageLoad.url)

            val result = route(application, request).value

            val expectedPayload = TraderProfile(testEori, "1", None, None)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
            verify(mockConnector, times(1)).submitTraderProfile(eqTo(expectedPayload), eqTo(testEori))(any())

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, times(1))
                .auditProfileSetUp(eqTo(expectedPayload), eqTo(AffinityGroup.Individual))(any())
            }
          }
        }
      }

      "when user answers cannot create a trader profile" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector    = mock[TraderProfileConnector]
          val mockAuditService = mock[AuditService]
          val continueUrl      = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCreateProfileController.onPageLoad.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
            verify(mockConnector, never()).submitTraderProfile(any(), any())(any())

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditProfileSetUp(any(), any())(any())
            }
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatoryProfileUserAnswers

        val mockConnector = mock[TraderProfileConnector]
        when(mockConnector.submitTraderProfile(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateProfileController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }
        }
      }

      "must let the play error handler deal with an audit future failure" in {

        val userAnswers = mandatoryProfileUserAnswers

        val mockConnector = mock[TraderProfileConnector]
        when(mockConnector.submitTraderProfile(any(), any())(any())).thenReturn(Future.successful(Done))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditProfileSetUp(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Audit failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateProfileController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateProfileController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
