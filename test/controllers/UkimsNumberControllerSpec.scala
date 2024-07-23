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
import forms.UkimsNumberFormProvider
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.UkimsNumberView

import scala.concurrent.Future

class UkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new UkimsNumberFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  "UkimsNumberController" - {

    ".create" - {

      val ukimsNumberRoute = routes.UkimsNumberController.onPageLoadCreate(NormalMode).url

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UkimsNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, routes.UkimsNumberController.onSubmitCreate(NormalMode))(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(UkimsNumberPage, "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill("answer"),
            routes.UkimsNumberController.onSubmitCreate(NormalMode)
          )(
            request,
            messages(application)
          ).toString
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
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "XIUKIM47699357400020231115081800"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, routes.UkimsNumberController.onSubmitCreate(NormalMode))(
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
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Homepage for a GET if profile already exists" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".update" - {

      val ukimsNumberRoute = routes.UkimsNumberController.onPageLoadUpdate.url

      "must return OK and the correct view for a GET with all trader profile complete" in {

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
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UkimsNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("1"), routes.UkimsNumberController.onSubmitUpdate)(
            request,
            messages(application)
          ).toString

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must redirect to Profile for a POST and submit data if value is different from original" in {

        val traderProfile = TraderProfile(testEori, "1", Some("2"), Some("3"))

        val answer               = "XIUKIM47699357400020231115081800"
        val updatedTraderProfile = TraderProfile(testEori, answer, Some("2"), Some("3"))

        val userAnswers = emptyUserAnswers
          .set(UkimsNumberUpdatePage, answer)
          .success
          .value

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditMaintainProfile(any(), any(), any())(any))
          .thenReturn(Future.successful(Done))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

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
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", answer))

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

      "must redirect to Profile for a POST and not submit data if value is the same as original" in {

        val traderProfile = TraderProfile(testEori, "XIUKIM47699357400020231115081800", Some("2"), Some("3"))

        val answer = "XIUKIM47699357400020231115081800"

        val userAnswers = emptyUserAnswers
          .set(UkimsNumberUpdatePage, answer)
          .success
          .value

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        val mockAuditService                                   = mock[AuditService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockTraderProfileConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

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
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", answer))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockTraderProfileConnector, never()).submitTraderProfile(any(), any())(any())

          withClue("must not try and submit an audit") {
            verify(mockAuditService, never()).auditMaintainProfile(any(), any(), any())(any())
          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[UkimsNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, routes.UkimsNumberController.onSubmitUpdate)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, ukimsNumberRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, ukimsNumberRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
