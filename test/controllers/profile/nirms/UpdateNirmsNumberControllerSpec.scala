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

package controllers.profile.nirms

import base.SpecBase
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import controllers.profile.nirms.routes.*
import controllers.profile.routes.*
import forms.profile.nirms.NirmsNumberFormProvider
import models.{NormalMode, TraderProfile}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.nirms.{HasNirmsUpdatePage, NirmsNumberUpdatePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.profile.NirmsNumberView

import scala.concurrent.Future
class UpdateNirmsNumberControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new NirmsNumberFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  "UpdateNirmsNumberController" - {

    val nirmsNumberRoute = UpdateNirmsNumberController.onPageLoad(NormalMode).url

    "must return OK and the correct view for a GET when HasNirms hasn't been answered when there is a nirms number" in {

      val traderProfile    = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(
        true
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NirmsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("2"),
          UpdateNirmsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return OK and the correct view for a GET when HasNirms has been answered when there is a nirms number" in {

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)

      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(
        true
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNirmsUpdatePage, true).success.value))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NirmsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("2"),
          UpdateNirmsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return OK and the correct view for a GET when HasNirms has been answered when there isn't a nirms number" in {

      val traderProfile = TraderProfile(testEori, "1", None, Some("3"), eoriChanged = false)

      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(
        true
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNirmsUpdatePage, true).success.value))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NirmsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          UpdateNirmsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must redirect to CyaMaintainProfile for a POST" in {
      val answer = "RMS-GB-123456"

      val traderProfile = TraderProfile(testEori, "1", Some(answer), Some("3"), eoriChanged = false)

      val userAnswers = emptyUserAnswers
        .set(HasNirmsUpdatePage, true)
        .success
        .value
        .set(NirmsNumberUpdatePage, answer)
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, nirmsNumberRoute)
            .withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CyaMaintainProfileController
          .onPageLoadNirmsNumber()
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, nirmsNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[NirmsNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          UpdateNirmsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if HasNirms hasn't been answered and trader profile has no nirms number" in {

      val traderProfile    = TraderProfile(testEori, "1", None, Some("3"), eoriChanged = false)
      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(
        true
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      val expectedRedirectLocation =
        controllers.problem.routes.JourneyRecoveryController
          .onPageLoad(
            Some(RedirectUrl(ProfileController.onPageLoad().url))
          )
          .url

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectLocation
      }
    }

    "must redirect to Journey Recovery for a GET if HasNirms has been answered no" in {

      val traderProfile    = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(
        true
      )

      val userAnswers = emptyUserAnswers.set(HasNirmsUpdatePage, false).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      val expectedRedirectLocation =
        controllers.problem.routes.JourneyRecoveryController
          .onPageLoad(
            Some(RedirectUrl(ProfileController.onPageLoad().url))
          )
          .url

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectLocation
        verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, nirmsNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, nirmsNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
