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
import connectors.{DownloadDataConnector, GoodsRecordConnector}
import models.Email
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.download.FileManagementViewModel
import views.html.download.FileManagementView

import java.time.Instant
import scala.concurrent.Future

class FileManagementControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val fileManagementRoute = controllers.download.routes.FileManagementController.onPageLoad().url

  private val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]
  private val mockGoodsRecordConnector: GoodsRecordConnector   = mock[GoodsRecordConnector]

  override def beforeEach(): Unit = {

    reset(mockDownloadDataConnector)

    super.beforeEach()
  }

  "FileManagementController" - {

    "when download data feature is enabled" - {
      "must return OK and view for a GET and email is verified" in {

        when(mockDownloadDataConnector.getDownloadDataSummary(any())) thenReturn Future.successful(Seq.empty)
        when(mockDownloadDataConnector.getDownloadData(any())) thenReturn Future.successful(Seq.empty)
        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(
          Some(Email("address", Instant.now()))
        )
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(mockGoodsRecordConnector.getRecords(any[Int], any[Int])(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[DownloadDataConnector].toInstance(mockDownloadDataConnector),
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
          )
          .build()

        implicit val messagesImplicit: Messages = messages(application)

        val viewModel = FileManagementViewModel(None, None, true)

        running(application) {
          val request = FakeRequest(GET, fileManagementRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[FileManagementView]

          status(result) mustEqual OK
          contentAsString(result) must include("TGP records files")
        }

        verify(mockDownloadDataConnector, atLeastOnce()).getDownloadDataSummary(any())
        verify(mockDownloadDataConnector, atLeastOnce()).getDownloadData(any())
        verify(mockGoodsRecordConnector, atLeastOnce()).getRecords(any[Int], any[Int])(any[HeaderCarrier])
      }
      "must return SEE_OTHER and redirect to IndexController if email is unverified" in {

        when(mockDownloadDataConnector.getEmail(any())) thenReturn Future.successful(None)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, fileManagementRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.IndexController.onPageLoad().url)
        }

        verify(mockDownloadDataConnector, never()).getDownloadDataSummary(any())
        verify(mockDownloadDataConnector, never()).getDownloadData(any())
      }
    }

    "must return SEE_OTHER and redirect to journey recovery if download feature is disabled" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .configure("features.download-file-enabled" -> false)
        .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileManagementRoute)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }

      verify(mockDownloadDataConnector, never()).getDownloadDataSummary(any())
      verify(mockDownloadDataConnector, never()).getDownloadData(any())
    }

  }
}
