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
import models.GoodsRecordsPagination.firstPage
import models.router.responses.GetRecordsResponse
import models.{DownloadDataSummary, Email, FileInfo, GoodsRecordsPagination, HistoricProfileData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SessionData.{newUkimsNumberPage, pageUpdated}
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

      val historicProfileData = HistoricProfileData("GB123456789", "GB123456789", Some("XIUKIMS1234567890"), None, None)

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
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          Some(historicProfileData)
        )

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          downloadDataSummary
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
            downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested",
            ukimsNumberChanged = false,
            doesGoodsRecordExist = true,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }

      "must return OK and the correct view for a GET without banner" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          Some(historicProfileData)
        )

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
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          downloadDataSummary
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
            downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested",
            ukimsNumberChanged = false,
            doesGoodsRecordExist = true,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }

      "must return OK and the correct view for a GET with correct messageKey" - {
        "when downloadDataSummary is None" in {
          val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
          when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
          when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
            Some(historicProfileData)
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
            Seq.empty
          )
          when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
            Some(Email("address", Instant.now()))
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
              downloadLinkMessagesKey = "homepage.downloadLinkText.noFilesRequested",
              ukimsNumberChanged = false,
              doesGoodsRecordExist = true,
              viewUpdateGoodsRecordsLink =
                controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
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
          when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
          when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
            Some(historicProfileData)
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
            downloadDataSummary
          )
          when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
            Some(Email("address", Instant.now()))
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested",
              ukimsNumberChanged = false,
              doesGoodsRecordExist = true,
              viewUpdateGoodsRecordsLink =
                controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
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
          when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
          when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
            Some(historicProfileData)
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
            downloadDataSummary
          )
          when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
            Some(Email("address", Instant.now()))
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested",
              ukimsNumberChanged = false,
              doesGoodsRecordExist = true,
              viewUpdateGoodsRecordsLink =
                controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
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
          when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
          when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
            Some(historicProfileData)
          )

          val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
          when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
            downloadDataSummary
          )
          when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
            Some(Email("address", Instant.now()))
          )

          val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
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
              downloadLinkMessagesKey = "homepage.downloadLinkText.filesRequested",
              ukimsNumberChanged = false,
              doesGoodsRecordExist = true,
              viewUpdateGoodsRecordsLink =
                controllers.goodsProfile.routes.PreviousMovementRecordsController.onPageLoad().url
            )(request, messages(application)).toString

            verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
            verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
            verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
          }
        }
      }
    }

    "when there are not any goods records" - {
      "must return OK and the correct view for a GET with noGoodsRecords messageKey" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          None
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          Seq.empty
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
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
            downloadLinkMessagesKey = "homepage.noRecords",
            ukimsNumberChanged = false,
            doesGoodsRecordExist = false,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }
    }

    "when email is not verified" - {
      "must return OK and the correct view for a GET with unverifiedEmail messageKey" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          None
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          Seq.empty
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          None
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
            downloadLinkMessagesKey = "homepage.downloadLinkText.unverifiedEmail",
            ukimsNumberChanged = false,
            doesGoodsRecordExist = false,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }
    }

    "ukimsNumberChanged should be" - {
      "false when pageUpdated does not contain a newUKIMS value" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          None
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          Seq.empty
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
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
            downloadLinkMessagesKey = "homepage.noRecords",
            ukimsNumberChanged = false,
            doesGoodsRecordExist = false,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }
      "true when pageUpdated contains a newUKIMS value" in {

        val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
        when(mockTraderProfileConnector.checkTraderProfile(any())) thenReturn Future.successful(true)
        when(mockTraderProfileConnector.getHistoricProfileData(any())(any())) thenReturn Future.successful(
          None
        )

        val mockGoodsRecordConnector: GoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecords(any(), any())(any())) thenReturn Future
          .successful(None)

        val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(
          Seq.empty
        )
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
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
            .withSession(pageUpdated -> newUkimsNumberPage)

          val result  = route(application, request).value

          val view = application.injector.instanceOf[HomePageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            downloadReady = false,
            downloadLinkMessagesKey = "homepage.noRecords",
            ukimsNumberChanged = true,
            doesGoodsRecordExist = false,
            viewUpdateGoodsRecordsLink =
              controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage).url
          )(request, messages(application)).toString

          verify(mockTraderProfileConnector, never()).checkTraderProfile(any())
          verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any(), any())(any())
          verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        }
      }

    }
  }
}
