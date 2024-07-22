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
import forms.RemoveNirmsFormProvider
import models.TraderProfile
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.HasNirmsUpdatePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.RemoveNirmsView

import scala.concurrent.Future

class RemoveNirmsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider                       = new RemoveNirmsFormProvider()
  private val form                       = formProvider()
  private val mockSessionRepository      = mock[SessionRepository]
  private val mockTraderProfileConnector = mock[TraderProfileConnector]

  private lazy val removeNirmsRoute = routes.RemoveNirmsController.onPageLoad().url

  "RemoveNirms Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockAuditService = mock[AuditService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeNirmsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveNirmsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must redirect to the next page when No submitted and not submit" in {
      val mockAuditService = mock[AuditService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockTraderProfileConnector = mock[TraderProfileConnector]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockTraderProfileConnector, never()).submitTraderProfile(any(), any())(any())

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must redirect to the next page when Yes submitted and submit" in {
      val mockAuditService = mock[AuditService]

      when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
        .thenReturn(Future.successful(Done))

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

      val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"))
      val updatedTraderProfile = TraderProfile(testEori, "1", None, Some("3"))
      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNirmsUpdatePage, false).success.value))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockTraderProfileConnector)
          .submitTraderProfile(eqTo(updatedTraderProfile), eqTo(testEori))(any())

        withClue("must call the audit connector with the supplied details") {
          verify(mockAuditService)
            .auditMaintainProfile(
              eqTo(traderProfile),
              eqTo(updatedTraderProfile),
              eqTo(AffinityGroup.Individual)
            )(
              any()
            )
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveNirmsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeNirmsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if Trader Profile cannot be built" in {
      val mockAuditService = mock[AuditService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeNirmsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val continueUrl = RedirectUrl(routes.HasNirmsController.onPageLoadUpdate.url)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }
  }
}
