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

package controllers.newUkims

import base.SpecBase
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import forms.profile.ukims.UkimsNumberFormProvider
import models.{NormalMode, TraderProfile, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.newUkims.NewUkimsNumberPage
import play.api.data.FormError
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.newUkims.NewUkimsNumberView

import scala.concurrent.Future

class NewUkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new UkimsNumberFormProvider()

  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  "NewUkimsNumberController" - {

    val newUkimsNumberRoute = controllers.newUkims.routes.NewUkimsNumberController.onPageLoad(NormalMode).url

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {

        val request = FakeRequest(GET, newUkimsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NewUkimsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          controllers.newUkims.routes.NewUkimsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(NewUkimsNumberPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, newUkimsNumberRoute)

        val view = application.injector.instanceOf[NewUkimsNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("answer"),
          controllers.newUkims.routes.NewUkimsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

      val mockSessionRepository: SessionRepository = mock[SessionRepository]

      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val traderProfile =
        TraderProfile(testEori, "XIUKIM47699357400020231115081801", Some("2"), Some("3"), eoriChanged = false)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, newUkimsNumberRoute)
            .withFormUrlEncodedBody(("value", "XIUKIM47699357400020231115081800"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CyaNewUkimsNumberController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, newUkimsNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[NewUkimsNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          controllers.newUkims.routes.NewUkimsNumberController.onSubmit(NormalMode)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when the same UKIMS number is submitted" in {

      val traderProfile =
        TraderProfile(testEori, "XIUKIM47699357400020231115081800", Some("2"), Some("3"), eoriChanged = false)

      when(mockTraderProfileConnector.getTraderProfile(any())) thenReturn Future.successful(
        traderProfile
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, newUkimsNumberRoute)
            .withFormUrlEncodedBody(("value", "XIUKIM47699357400020231115081800"))

        val boundForm = form
          .fill("XIUKIM47699357400020231115081800")
          .copy(errors =
            Seq(elems = FormError("value", "You’ve entered the previous UKIMS number. Enter the new UKIMS number."))
          )

        val view = application.injector.instanceOf[NewUkimsNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          controllers.newUkims.routes.NewUkimsNumberController.onSubmit(NormalMode)
        )(
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
        val request = FakeRequest(GET, newUkimsNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, newUkimsNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
