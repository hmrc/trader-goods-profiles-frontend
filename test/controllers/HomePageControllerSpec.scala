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
import connectors.{DownloadDataConnector, GoodsRecordConnector, TraderProfileConnector}
import models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen}
import models.router.responses.GetRecordsResponse
import models.{DownloadDataSummary, FileInfo, GoodsRecordsPagination}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.HomePageView

import java.time.Instant
import scala.concurrent.Future

class HomePageControllerSpec extends SpecBase {

  "HomePage Controller" - {

    val fileName      = "fileName"
    val fileSize      = 600
    val retentionDays = "30"

    "when there are goods records" - {

      val records = Seq(
        goodsRecordResponse(
          Instant.parse("2022-11-18T23:20:19Z"),
          Instant.parse("2022-11-18T23:20:19Z")
        )
      )

      val goodsResponse = GetRecordsResponse(
        records,
        GoodsRecordsPagination(1, 1, 1, None, None)
      )

      "must return OK and the correct view for a GET with banner" in {

        val downloadDataSummary = Seq(
          DownloadDataSummary(
            "id",
            testEori,
            FileReadyUnseen,
            Instant.now(),
            Instant.now(),
            Some(FileInfo(fileName, fileSize, retentionDays))
          )
        )

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          downloadDataSummary
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
          .successful(Some(goodsResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = true,
            downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested"
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
        }
      }

      "must return OK and the correct view for a GET without banner" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val downloadDataSummary = Seq(
          DownloadDataSummary(
            "id",
            testEori,
            FileReadySeen,
            Instant.now(),
            Instant.now(),
            Some(FileInfo(fileName, fileSize, retentionDays))
          )
        )

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          downloadDataSummary
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
          .successful(Some(goodsResponse))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = false,
            downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested"
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
        }
      }

      "must return OK and the correct view for a GET with correct messageKey" - {
        "when downloadDataSummary is None" in {
          val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            Seq.empty
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
            .successful(Some(goodsResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HomePageView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              downloadReady = false,
              downloadLinkMessagesKey = "homepage.downloadLinkText.noFilesRequested"
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
          }
        }

        "when downloadDataSummary is FileInProgress" in {
          val downloadDataSummary = Seq(
            DownloadDataSummary(
              "id",
              testEori,
              FileInProgress,
              Instant.now(),
              Instant.now(),
              None
            )
          )

          val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            downloadDataSummary
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
            .successful(Some(goodsResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HomePageView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              downloadReady = false,
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested"
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
          }
        }

        "when downloadDataSummary is FileReadyUnseen" in {
          val downloadDataSummary = Seq(
            DownloadDataSummary(
              "id",
              testEori,
              FileReadyUnseen,
              Instant.now(),
              Instant.now(),
              None
            )
          )

          val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            downloadDataSummary
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
            .successful(Some(goodsResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HomePageView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              downloadReady = true,
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested"
            )(request, messages(application)).toString
          }
        }

        "when downloadDataSummary is FileReadySeen" in {
          val downloadDataSummary = Seq(
            DownloadDataSummary(
              "id",
              testEori,
              FileReadySeen,
              Instant.now(),
              Instant.now(),
              None
            )
          )

          val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
          when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
            downloadDataSummary
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
            .successful(Some(goodsResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HomePageView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              downloadReady = false,
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested"
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
          }
        }
      }
    }

    "when there are not any goods records" - {
      "must return OK and the correct view for a GET with noGoodsRecords messageKey" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          Seq.empty
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = false,
            downloadLinkMessagesKey = "homepage.downloadLinkText.noGoodsRecords"
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
        }
      }
    }

    "ukimsNumberChanged should be" - {
      "false when pageUpdated does not contain a newUKIMS value" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          Seq.empty
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = false,
            downloadLinkMessagesKey = "homepage.downloadLinkText.noGoodsRecords",
            ukimsNumberChanged = false
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
        }
      }
      "true when pageUpdated contains a newUKIMS value" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
          Seq.empty
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.HomePageController.onPageLoad().url)
            .withSession("pageUpdated" -> "newUkimsNumberUpdatePage")

          val result  = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = false,
            downloadLinkMessagesKey = "homepage.downloadLinkText.noGoodsRecords",
            ukimsNumberChanged = false
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())(any())
        }
      }

    }
  }
}
