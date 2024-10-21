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
import models.DownloadDataStatus.{FileReadySeen, RequestFile}
import models.{DownloadData, DownloadDataSummary, FileInfo, Metadata}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.FileReadyView

import java.time.Instant
import scala.concurrent.Future

class FileReadyControllerSpec extends SpecBase with MockitoSugar {

  private lazy val fileReadyRoute = routes.FileReadyController.onPageLoad().url

  "FileReadyController" - {

    val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
    when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

    "onPageLoad" - {

      "must OK and display correct view when link is got successfully" in {

        val fileName                 = "fileName"
        val fileSize                 = 600
        val fileCreated              = Instant.parse("2025-03-01T18:35:24.00Z")
        val retentionDays            = "30"
        val url                      = "/some-url"
        val fileRoleMetadata         = Metadata("FileRole", "C79Certificate")
        val periodStartYearMetadata  = Metadata("PeriodStartYear", "2020")
        val retentionDaysMetadata    = Metadata("RETENTION_DAYS", retentionDays)
        val periodStartMonthMetadata = Metadata("PeriodStartMonth", "08")
        val createdDate              = "1 March 2025"
        val availableUntil           = "31 March 2025"
        val downloadData             = DownloadData(
          url,
          fileName,
          fileSize,
          Seq(
            fileRoleMetadata,
            periodStartYearMetadata,
            retentionDaysMetadata,
            periodStartMonthMetadata
          )
        )
        val downloadDataSummary      = DownloadDataSummary(
          testEori,
          FileReadySeen,
          Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
        )

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          Some(Seq(downloadDataSummary))
        )
        when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(
          Some(Seq(downloadData))
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, fileReadyRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[FileReadyView]
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(fileSize, url, createdDate, availableUntil)(
            request,
            messages(application)
          ).toString

          verify(mockDownloadDataConnector).getDownloadDataSummary(eqTo(testEori))(any())
          verify(mockDownloadDataConnector).getDownloadData(eqTo(testEori))(any())

        }

      }

      "redirect to journey recovery if the link cannot be retrieved" - {

        "because download data is not found" in {

          val fileName      = "fileName"
          val fileSize      = 600
          val fileCreated   = Instant.parse("2025-03-01T18:35:24.00Z")
          val retentionDays = "30"

          val downloadDataSummary = DownloadDataSummary(
            testEori,
            FileReadySeen,
            Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            Some(Seq(downloadDataSummary))
          )
          when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, fileReadyRoute)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad()
              .url

            verify(mockDownloadDataConnector).getDownloadDataSummary(eqTo(testEori))(any())
            verify(mockDownloadDataConnector).getDownloadData(eqTo(testEori))(any())
          }
        }

        "because fileInfo is not present" in {

          val downloadDataSummary = DownloadDataSummary(
            testEori,
            FileReadySeen,
            None
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            Some(Seq(downloadDataSummary))
          )
          when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, fileReadyRoute)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad()
              .url

            verify(mockDownloadDataConnector).getDownloadDataSummary(eqTo(testEori))(any())
            verify(mockDownloadDataConnector, never()).getDownloadData(eqTo(testEori))(any())
          }
        }

        "because status is not FileReady (seen or unseen)" in {

          val downloadDataSummary = DownloadDataSummary(
            testEori,
            RequestFile,
            None
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            Some(Seq(downloadDataSummary))
          )
          when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, fileReadyRoute)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad()
              .url

            verify(mockDownloadDataConnector).getDownloadDataSummary(eqTo(testEori))(any())
            verify(mockDownloadDataConnector, never()).getDownloadData(eqTo(testEori))(any())
          }
        }

        "because summary is not found" in {

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            None
          )
          when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
            .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, fileReadyRoute)
            val result  = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad()
              .url

            verify(mockDownloadDataConnector).getDownloadDataSummary(eqTo(testEori))(any())
            verify(mockDownloadDataConnector, never()).getDownloadData(eqTo(testEori))(any())
          }
        }
      }
    }
  }
}
