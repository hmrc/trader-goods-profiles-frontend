/*
 * Copyright 2023 HM Revenue & Customs
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

package viewmodels

import base.SpecBase
import connectors.DownloadDataConnector
import generators.Generators
import models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import models.{DownloadData, DownloadDataSummary, FileInfo}
import models.filemanagement.{AvailableFilesTable, FileManagementTable, PendingFilesTable}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Messages
import play.api.inject.bind
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContextExecutor, Future}

class FileManagementViewModelSpec extends SpecBase with Generators {

  "FileManagementViewModel" - { // TODO: Test apply object

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    val availableFilesTableRow = arbitrarySeqTableRows.sample.value

    val pendingFilesTableRow = arbitrarySeqTableRows.sample.value

    implicit val message: Messages = messages(application)

    val viewModelAvailableFiles = FileManagementViewModel(Some(AvailableFilesTable(availableFilesTableRow)), None)
    val viewModelPendingFiles   = FileManagementViewModel(None, Some(PendingFilesTable(pendingFilesTableRow)))
    val viewModelNoFiles        = FileManagementViewModel(None, None)
    val viewModelAllFiles       = FileManagementViewModel(
      Some(AvailableFilesTable(availableFilesTableRow)),
      Some(PendingFilesTable(pendingFilesTableRow))
    )
    val viewModels              =
      Gen.oneOf(Seq(viewModelAvailableFiles, viewModelPendingFiles, viewModelNoFiles, viewModelAllFiles)).sample.value

    "isFiles" - {

      "must return true if both tables are defined" in {
        viewModelAllFiles.isFiles mustEqual true
      }

      "must return true if availableFilesTable is defined" in {
        viewModelAvailableFiles.isFiles mustEqual true
      }

      "must return true if pendingFilesTable is defined" in {
        viewModelPendingFiles.isFiles mustEqual true
      }

      "must return false if both pendingFilesTable and availableFilesTable" in {
        viewModelNoFiles.isFiles mustEqual false
      }
    }

    "title" - {
      "must return the correct title" in {
        viewModels.title mustEqual "TGP records files"
      }
    }

    "heading" - {
      "must return the correct heading" in {
        viewModels.heading mustEqual "TGP records files"
      }
    }

    "paragraph1" - {
      "must return the correct paragraph1 when files are available" in {
        viewModelAllFiles.paragraph1 mustEqual "Files are available for 30 days from when we email you that it is ready to download."
      }

      "must return the correct paragraph1 when no files are available" in {
        viewModelNoFiles.paragraph1 mustEqual "You do not have any TGP record files ready for download. Files expire after 30 days."
      }
    }

    "tgpRecordsLink" - {
      "must return the correct link text" in {
        viewModels.tgpRecordsLink mustEqual "Get a new TGP records file"
      }
    }

    "goBackHomeLink" - {
      "must return the correct link text" in {
        viewModels.goBackHomeLink mustEqual "Go back to homepage"
      }
    }

    "FileManagementViewModelProvider" - {
      ".apply" - {
        "must return a FileManagementViewModel" - {
          "with correct values" - {
            "when there is no data" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(None)
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              result.pendingFilesTable mustBe None
              result.availableFilesTable mustBe None
            }

            "when all files are pending" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              val downloadDataSummary = Some(Seq(DownloadDataSummary("eori", FileInProgress, Instant.now(), None)))

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
                downloadDataSummary
              )
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(None)

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              val pendingFilesTable = FileManagementTable.PendingFilesTable(downloadDataSummary)

              result.pendingFilesTable mustBe pendingFilesTable
              result.availableFilesTable mustBe None
            }

            "when all files are available" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              val fileName = "file"

              val fileInfo            = FileInfo(fileName, 1, Instant.now(), "30")
              val downloadDataSummary = DownloadDataSummary("eori", FileReadyUnseen, Instant.now(), Some(fileInfo))
              val downloadData        = DownloadData("file", fileName, 1, Seq.empty)

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
                Some(Seq(downloadDataSummary))
              )
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(
                Some(Seq(downloadData))
              )

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              val availableFilesTable =
                FileManagementTable.AvailableFilesTable(Some(Seq((downloadDataSummary, downloadData))))

              result.pendingFilesTable mustBe None
              result.availableFilesTable mustBe availableFilesTable
            }

            "when both files are available and pending" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              val fileName = "file"

              val fileInfo            = FileInfo(fileName, 1, Instant.now(), "30")
              val downloadDataSummary = Seq(
                DownloadDataSummary("eori", FileReadyUnseen, Instant.now(), Some(fileInfo)),
                DownloadDataSummary("eori", FileInProgress, Instant.now(), None)
              )

              val downloadData = DownloadData("file", fileName, 1, Seq.empty)

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
                Some(downloadDataSummary)
              )
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(
                Some(Seq(downloadData))
              )

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              val availableFilesTable =
                FileManagementTable.AvailableFilesTable(Some(Seq((downloadDataSummary.head, downloadData))))

              val pendingFilesTable = FileManagementTable.PendingFilesTable(Some(Seq(downloadDataSummary.last)))

              result.pendingFilesTable mustBe pendingFilesTable
              result.availableFilesTable mustBe availableFilesTable
            }

            "when files should be available, but there is no DownloadData" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              val fileName = "file"

              val fileInfo            = FileInfo(fileName, 1, Instant.now(), "30")
              val downloadDataSummary = Seq(
                DownloadDataSummary("eori", FileReadyUnseen, Instant.now(), Some(fileInfo))
              )

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
                Some(downloadDataSummary)
              )
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(
                None
              )

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              result.pendingFilesTable mustBe None
              result.availableFilesTable mustBe None
            }

            "when files should be available, but there is no DownloadData with a matching fileName" in {
              val viewModelProvider = new FileManagementViewModel.FileManagementViewModelProvider()

              val mockDownloadDataConnector: DownloadDataConnector = mock[DownloadDataConnector]

              val fileName = "file"

              val fileInfo            = FileInfo(fileName, 1, Instant.now(), "30")
              val downloadDataSummary = Seq(
                DownloadDataSummary("eori", FileReadyUnseen, Instant.now(), Some(fileInfo))
              )
              val downloadData        = DownloadData("unmatched", "unmatched", 1, Seq.empty)

              when(mockDownloadDataConnector.getDownloadDataSummary(any())(any())) thenReturn Future.successful(
                Some(downloadDataSummary)
              )
              when(mockDownloadDataConnector.getDownloadData(any())(any())) thenReturn Future.successful(
                Some(Seq(downloadData))
              )

              val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[DownloadDataConnector].toInstance(mockDownloadDataConnector))
                .build()

              val messagesImp: Messages        = messages(application)
              val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
              val hc: HeaderCarrier            = HeaderCarrier()

              val result = viewModelProvider.apply("eori", mockDownloadDataConnector)(messagesImp, ec, hc).futureValue

              result.pendingFilesTable mustBe None
              result.availableFilesTable mustBe None
            }
          }
        }
      }
    }
  }
}
