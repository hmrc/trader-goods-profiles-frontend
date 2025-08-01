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

package controllers.profile.niphl

import base.SpecBase
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import controllers.profile.niphl.routes.*
import forms.profile.niphl.NiphlNumberFormProvider
import models.{NormalMode, TraderProfile}
import navigation.{FakeProfileNavigator, ProfileNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.profile.niphl.{HasNiphlUpdatePage, NiphlNumberUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AuditService
import views.html.profile.NiphlNumberView

import scala.concurrent.Future

class UpdateNiphlNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new NiphlNumberFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  private lazy val niphlNumberRouteUpdate =
    UpdateNiphlNumberController.onPageLoad(NormalMode).url

  "UpdateNiphlNumberControllerSpec" - {
    "must return OK and the correct view for a GET when HasNiphl hasn't been answered when there is a niphl number" in {
      val traderProfile    = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, niphlNumberRouteUpdate)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NiphlNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("3"),
          UpdateNiphlNumberController.onSubmit(NormalMode)
        )(request, messages(application)).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return OK and the correct view for a GET when HasNiphl hasn't been answered when there isn't a niphl number" in {
      val traderProfile    = TraderProfile(testEori, "1", Some("2"), None, eoriChanged = false)
      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, niphlNumberRouteUpdate)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NiphlNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          UpdateNiphlNumberController.onSubmit(NormalMode)
        )(request, messages(application)).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return OK and the correct view for a GET when HasNiphl has been answered when there is a niphl number" in {
      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)

      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNiphlUpdatePage, true).success.value))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, niphlNumberRouteUpdate)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NiphlNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("3"),
          UpdateNiphlNumberController.onSubmit(NormalMode)
        )(request, messages(application)).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return OK and the correct view for a GET when HasNiphl has been answered when there isn't a niphl number" in {
      val traderProfile = TraderProfile(testEori, "1", Some("3"), None, eoriChanged = false)

      val mockAuditService = mock[AuditService]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(HasNiphlUpdatePage, true).success.value))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, niphlNumberRouteUpdate)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NiphlNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          UpdateNiphlNumberController.onSubmit(NormalMode)
        )(request, messages(application)).toString

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must redirect to Profile for a POST and submit data if value is different from original" in {
      val answer = "SN12345"

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false)
      val userAnswers   = emptyUserAnswers
        .set(HasNiphlUpdatePage, true)
        .success
        .value
        .set(NiphlNumberUpdatePage, answer)
        .success
        .value

      val mockAuditService = mock[AuditService]

      when(mockAuditService.auditMaintainProfile(any(), any(), any())(any)).thenReturn(Future.successful(Done))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[ProfileNavigator].toInstance(new FakeProfileNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, niphlNumberRouteUpdate).withFormUrlEncodedBody(("value", answer))
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Profile for a POST and not submit data if value is the same as original" in {
      val answer = "SN12345"

      val traderProfile = TraderProfile(testEori, "1", Some("2"), Some(answer), eoriChanged = false)

      val userAnswers = emptyUserAnswers
        .set(HasNiphlUpdatePage, true)
        .success
        .value
        .set(NiphlNumberUpdatePage, answer)
        .success
        .value

      val mockAuditService = mock[AuditService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(traderProfile)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, niphlNumberRouteUpdate).withFormUrlEncodedBody(("value", answer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.profile.routes.CyaMaintainProfileController
          .onPageLoadNiphlNumber()
          .url
        verify(mockTraderProfileConnector, never()).submitTraderProfile(any())(any())

        withClue("must not try and submit an audit") {
          verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, niphlNumberRouteUpdate).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[NiphlNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          UpdateNiphlNumberController.onSubmit(NormalMode)
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, niphlNumberRouteUpdate)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, niphlNumberRouteUpdate)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
