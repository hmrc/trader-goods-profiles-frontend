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
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.RequestDataView

import scala.concurrent.Future

class RequestDataControllerSpec extends SpecBase {

  private val email       = "placeholder@email.com"
  private def onwardRoute = Call("GET", "/foo")

  "RequestData Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.RequestDataController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RequestDataView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(email)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when button clicked" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.RequestDataController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

  }
}
