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
import connectors.TraderProfileConnector
import models.TraderProfile
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase {

  "Index Controller" - {

    "must redirect to ProfileSetupController if no profile present" in {

      val mockConnector = mock[TraderProfileConnector]
      when(mockConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.ProfileSetupController.onPageLoad().url

        verify(mockConnector, times(1)).checkTraderProfile(any())(any())

      }
    }

    "must redirect to HomePageController if no profile present" in {

      val mockConnector = mock[TraderProfileConnector]
      when(mockConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(false))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.ProfileSetupController.onPageLoad().url
        verify(mockConnector, times(1)).checkTraderProfile(any())(any())
      }
    }
    "must redirect to HomePageController if no profile present and eori has not changed" in {

      val mockConnector = mock[TraderProfileConnector]
      when(mockConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
      when(mockConnector.getTraderProfile(any())(any()))
        .thenReturn(
          Future.successful(TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = false))
        )

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
        verify(mockConnector, times(1)).checkTraderProfile(any())(any())
        verify(mockConnector, times(1)).getTraderProfile(any())(any())
      }
    }
    "must redirect to UkimsNumberChangeController if no profile present and eori has changed" in {

      val mockConnector = mock[TraderProfileConnector]
      when(mockConnector.checkTraderProfile(any())(any())).thenReturn(Future.successful(true))
      when(mockConnector.getTraderProfile(any())(any()))
        .thenReturn(
          Future.successful(TraderProfile("name", "address", Some("postcode"), Some("country"), eoriChanged = true))
        )

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.UkimsNumberChangeController.onPageLoad().url
        verify(mockConnector, times(1)).checkTraderProfile(any())(any())
        verify(mockConnector, times(1)).getTraderProfile(any())(any())
      }
    }
  }
}
