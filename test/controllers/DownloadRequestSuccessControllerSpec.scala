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
import connectors.{DownloadDataConnector, TraderProfileConnector}
import models.Email
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.DownloadRequestSuccessView

import java.time.Instant
import scala.concurrent.Future

class DownloadRequestSuccessControllerSpec extends SpecBase {

  "DownloadRequestSuccess Controller" - {

    "must return OK and the correct view for a GET" in {

      val address   = "somebody@email.com"
      val timestamp = Instant.now
      val email     = Email(address, timestamp)

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.getEmail(any())(any())) thenReturn Future.successful(email)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .overrides(inject.bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.DownloadRequestSuccessController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DownloadRequestSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(email.address)(
          request,
          messages(application)
        ).toString

        verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
        verify(mockDownloadDataConnector, atLeastOnce()).getEmail(any())(any())
      }
    }
  }
}
