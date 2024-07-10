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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.UkimsKickOutView

import scala.concurrent.Future

class UkimsKickOutControllerSpec extends SpecBase {

  "UkimsKickOut Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.UkimsKickOutController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UkimsKickOutView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to Home page for a GET if profile already exists" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.UkimsKickOutController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url
      }
    }
  }
}
