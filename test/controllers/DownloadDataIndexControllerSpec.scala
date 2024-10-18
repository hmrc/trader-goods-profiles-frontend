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
import connectors.DownloadDataConnector
import models.DownloadDataStatus._
import models.{DownloadDataSummary, FileInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}

import java.time.Instant
import scala.concurrent.Future

class DownloadDataIndexControllerSpec extends SpecBase with BeforeAndAfterEach {

  val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

  private def application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(inject.bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
    .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockDownloadDataConnector)
  }

  "DownloadDataIndexController" - {
    "must redirect to correct page" - {
      "when download summary is RequestFile" in {
        val downloadDataSummary =
          DownloadDataSummary("eori", RequestFile, Some(FileInfo("file", 1, Instant.now(), "30")))
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any()))
          .thenReturn(Future.successful(Some(downloadDataSummary)))

        val app = application

        running(app) {
          val request = FakeRequest(GET, routes.DownloadDataIndexController.redirect().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.RequestDataController.onPageLoad().url)
        }
      }

      "when download summary is FileInProgress" in {
        val downloadDataSummary =
          DownloadDataSummary("eori", FileInProgress, Some(FileInfo("file", 1, Instant.now(), "30")))
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any()))
          .thenReturn(Future.successful(Some(downloadDataSummary)))

        val app = application

        running(app) {
          val request = FakeRequest(GET, routes.DownloadDataIndexController.redirect().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.FileInProgressController.onPageLoad().url)
        }
      }

      "when download summary is FileReadyUnseen" in {
        val downloadDataSummary =
          DownloadDataSummary("eori", FileReadyUnseen, Some(FileInfo("file", 1, Instant.now(), "30")))
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any()))
          .thenReturn(Future.successful(Some(downloadDataSummary)))

        val app = application

        running(app) {
          val request = FakeRequest(GET, routes.DownloadDataIndexController.redirect().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.FileReadyController.onPageLoad().url)
        }
      }

      "when download summary is FileReadySeen" in {
        val downloadDataSummary =
          DownloadDataSummary("eori", FileReadySeen, Some(FileInfo("file", 1, Instant.now(), "30")))
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any()))
          .thenReturn(Future.successful(Some(downloadDataSummary)))

        val app = application

        running(app) {
          val request = FakeRequest(GET, routes.DownloadDataIndexController.redirect().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.FileReadyController.onPageLoad().url)
        }
      }
    }
  }
}
