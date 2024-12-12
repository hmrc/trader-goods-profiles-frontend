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

package controllers.download

import base.SpecBase
import connectors.{DownloadDataConnector, TraderProfileConnector}
import controllers.routes
import models.Email
import navigation.{DownloadNavigator, FakeDownloadNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.download.RequestDataView

import java.time.Instant
import scala.concurrent.Future

class RequestDataControllerSpec extends SpecBase {

  private def onwardRoute = Call("GET", "/foo")

  "RequestData Controller" - {

    "must redirect to Journey Recovery for a GET if no email is found" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.getEmail(any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .overrides(inject.bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.download.routes.RequestDataController.onPageLoad().url)

        val result = route(application, request).value

        val redirectUrl = Some(RedirectUrl(routes.IndexController.onPageLoad().url))

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
          .onPageLoad(redirectUrl)
          .url
      }
    }

    "must return OK and the correct view for a GET" in {

      val address   = "somebody@email.com"
      val timestamp = Instant.now
      val email     = Email(address, timestamp)

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .overrides(inject.bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.download.routes.RequestDataController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RequestDataView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(email.address)(request, messages(application)).toString

        verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
        verify(mockDownloadDataConnector, atLeastOnce()).getEmail(any())(any())
      }
    }

    "must redirect to the next page when button clicked and data requested" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.requestDownloadData(any())(any())) thenReturn Future.successful(Done)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[DownloadNavigator].toInstance(new FakeDownloadNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, controllers.download.routes.RequestDataController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
        verify(mockDownloadDataConnector, never()).getEmail(any())(any())
      }
    }
  }
}
