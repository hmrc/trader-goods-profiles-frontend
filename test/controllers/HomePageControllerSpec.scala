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
import base.TestConstants.testEori
import connectors.{DownloadDataConnector, TraderProfileConnector}
import models.DownloadDataStatus.{FileReadySeen, FileReadyUnseen}
import models.{DownloadDataSummary, FileInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.HomePageView

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class HomePageControllerSpec extends SpecBase {

  "HomePage Controller" - {

    val fileName      = "fileName"
    val fileSize      = 600
    val fileCreated   = Instant.now.minus(40, ChronoUnit.DAYS)
    val retentionDays = "30"
    val fileType      = "CSV"

    "must return OK and the correct view for a GET with banner" in {

      val downloadDataSummary = DownloadDataSummary(
        testEori,
        FileReadyUnseen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
        Some(downloadDataSummary)
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HomePageView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET without banner" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val downloadDataSummary = DownloadDataSummary(
        testEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
      when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
        Some(downloadDataSummary)
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HomePageView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(false)(request, messages(application)).toString
      }
    }
  }
}
