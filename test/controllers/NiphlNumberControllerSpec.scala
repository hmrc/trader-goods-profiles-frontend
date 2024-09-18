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
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import forms.NiphlNumberFormProvider
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasNiphlUpdatePage, NiphlNumberPage, NiphlNumberUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import views.html.NiphlNumberView

import scala.concurrent.Future

class NiphlNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new NiphlNumberFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  private lazy val niphlNumberRouteCreate = routes.NiphlNumberController.onPageLoadCreate(NormalMode).url

  private lazy val niphlNumberRouteUpdate = routes.NiphlNumberController.onPageLoadUpdate(NormalMode).url

  "NiphlNumber Controller" - {

    ".create" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, niphlNumberRouteCreate)

          val result = route(application, request).value

          val view = application.injector.instanceOf[NiphlNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, routes.NiphlNumberController.onSubmitCreate(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(NiphlNumberPage, "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, niphlNumberRouteCreate)

          val view = application.injector.instanceOf[NiphlNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("answer"),
            routes.NiphlNumberController.onSubmitCreate(NormalMode)
          )(request, messages(application)).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteCreate)
              .withFormUrlEncodedBody(("value", "SN12345"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteCreate)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[NiphlNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, routes.NiphlNumberController.onSubmitCreate(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, niphlNumberRouteCreate)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Homepage for a GET if profile already exists" in {

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, niphlNumberRouteCreate)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteCreate)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".update" - {

      "must return OK and the correct view for a GET when HasNiphl hasn't been answered when there is a niphl number" in {

        val traderProfile    = TraderProfile(testEori, "1", Some("2"), Some("3"))
        val mockAuditService = mock[AuditService]

        when(mockTraderProfileConnector.getTraderProfile(eqTo(testEori))(any())) thenReturn Future.successful(
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
          val request = FakeRequest(GET, niphlNumberRouteUpdate)

          val result = route(application, request).value

          val view = application.injector.instanceOf[NiphlNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("3"),
            routes.NiphlNumberController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return OK and the correct view for a GET when HasNiphl hasn't been answered when there isn't a niphl number" in {

        val traderProfile    = TraderProfile(testEori, "1", Some("2"), None)
        val mockAuditService = mock[AuditService]

        when(mockTraderProfileConnector.getTraderProfile(eqTo(testEori))(any())) thenReturn Future.successful(
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
          val request = FakeRequest(GET, niphlNumberRouteUpdate)

          val result = route(application, request).value

          val view = application.injector.instanceOf[NiphlNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, routes.NiphlNumberController.onSubmitUpdate(NormalMode))(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return OK and the correct view for a GET when HasNiphl has been answered when there is a niphl number" in {

        val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

        val mockAuditService = mock[AuditService]

        when(mockTraderProfileConnector.getTraderProfile(eqTo(testEori))(any())) thenReturn Future.successful(
          traderProfile
        )

        when(mockSessionRepository.set(any())) thenReturn Future.successful(
          true
        )

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
            routes.NiphlNumberController.onSubmitUpdate(NormalMode)
          )(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return OK and the correct view for a GET when HasNiphl has been answered when there isn't a niphl number" in {

        val traderProfile = TraderProfile(testEori, "1", Some("3"), None)

        val mockAuditService = mock[AuditService]

        when(mockTraderProfileConnector.getTraderProfile(eqTo(testEori))(any())) thenReturn Future.successful(
          traderProfile
        )

        when(mockSessionRepository.set(any())) thenReturn Future.successful(
          true
        )

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
          contentAsString(result) mustEqual view(form, routes.NiphlNumberController.onSubmitUpdate(NormalMode))(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must redirect to Profile for a POST and submit data if value is different from original" in {
        val answer = "SN12345"

        val traderProfile        = TraderProfile(testEori, "1", Some("2"), Some("3"))
        val updatedTraderProfile = TraderProfile(testEori, "1", Some("2"), Some(answer))
        val userAnswers          = emptyUserAnswers
          .set(HasNiphlUpdatePage, true)
          .success
          .value
          .set(NiphlNumberUpdatePage, answer)
          .success
          .value

        val mockAuditService = mock[AuditService]

        when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
          .thenReturn(Future.successful(Done))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteUpdate)
              .withFormUrlEncodedBody(("value", answer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect to Profile for a POST and not submit data if value is the same as original" in {
        val answer = "SN12345"

        val traderProfile = TraderProfile(testEori, "1", Some("2"), Some(answer))

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

        when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(traderProfile)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteUpdate)
              .withFormUrlEncodedBody(("value", answer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CyaMaintainProfileController.onPageLoadNiphl.url
          verify(mockTraderProfileConnector, never())
            .submitTraderProfile(any(), any())(any())

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, niphlNumberRouteUpdate)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[NiphlNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, routes.NiphlNumberController.onSubmitUpdate(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, niphlNumberRouteUpdate)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
