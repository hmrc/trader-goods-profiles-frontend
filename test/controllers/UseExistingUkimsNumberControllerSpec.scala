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
import forms.{UkimsNumberFormProvider, UseExistingUkimsFormProvider}
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{UkimsNumberPage, UkimsNumberUpdatePage, UseExistingUkimsPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.{UkimsNumberView, UseExistingUkimsNumberView}

import scala.concurrent.Future

class UseExistingUkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new UseExistingUkimsFormProvider()
  private val form         = formProvider()

  private val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  private val useExistingUkimsNumberRoute = routes.UseExistingUkimsNumberController.onPageLoad().url

  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

  private val ukimsNumber = "UKIMS123"

  private val userAnswersWithUkims = UserAnswers(userAnswersId)
    .set(UkimsNumberPage, ukimsNumber)
    .success
    .value

  "onPageLoad" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkims))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, useExistingUkimsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UseExistingUkimsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          routes.UseExistingUkimsNumberController.onSubmit(),
          ukimsNumber
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the journey recovery page when user answers doesn't have a ukims number pre-populated" in {
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, useExistingUkimsNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "onSubmit" - {

    "must return OK and the correct view for a POST" in {
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, useExistingUkimsNumberRoute).withFormUrlEncodedBody(("value", true))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithUkims)).build()

      running(application) {
        val request =
          FakeRequest(POST, useExistingUkimsNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[UseExistingUkimsNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          routes.UseExistingUkimsNumberController.onSubmit(),
          ukimsNumber
        )(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
