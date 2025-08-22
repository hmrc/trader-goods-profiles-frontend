/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase
import connectors.{DownloadDataConnector, GoodsRecordConnector}
import helpers.FileManagementTableComponentHelper
import models.*
import models.DownloadDataStatus.{FileFailedUnseen, FileInProgress, FileReadySeen, FileReadyUnseen}
import models.router.responses.GetRecordsResponse
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.Helpers.{await, defaultAwaitTimeout, stubMessagesApi}
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.download.FileManagementViewModel.FileManagementViewModelProvider

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class FileManagementViewModelProviderSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val messagesApi: MessagesApi    = stubMessagesApi()
  implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val defaultPagination: GoodsRecordsPagination = GoodsRecordsPagination(
    totalRecords = 0,
    currentPage = 1,
    totalPages = 1,
    nextPage = None,
    prevPage = None
  )

  val downloadDataConnector: DownloadDataConnector                           = mock[DownloadDataConnector]
  val goodsRecordConnector: GoodsRecordConnector                             = mock[GoodsRecordConnector]
  val fileManagementTableComponentHelper: FileManagementTableComponentHelper = mock[FileManagementTableComponentHelper]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(downloadDataConnector, goodsRecordConnector, fileManagementTableComponentHelper)
  }

  "FileManagementViewModelProvider" - {
    "when there are available files" - {
      "should create a FileManagementViewModel with available files table" in {

        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(downloadDataConnector.getDownloadDataSummary).thenReturn(
          Future.successful(
            Seq(
              DownloadDataSummary(
                "id1",
                "EORI123",
                FileReadySeen,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Some(FileInfo("file1.csv", 100, "30"))
              ),
              DownloadDataSummary(
                "id2",
                "EORI123",
                FileReadyUnseen,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Some(FileInfo("file2.csv", 100, "30"))
              )
            )
          )
        )
        when(downloadDataConnector.getDownloadData).thenReturn(
          Future.successful(
            Seq(
              DownloadData("url1", "file1.csv", 100, Seq()),
              DownloadData("url2", "file2.csv", 100, Seq())
            )
          )
        )
        when(goodsRecordConnector.getRecords(1, 1))
          .thenReturn(Future.successful(Some(GetRecordsResponse(Seq.empty, defaultPagination))))

        val provider = new FileManagementViewModelProvider(fileManagementTableComponentHelper, goodsRecordConnector)
        val result   = await(provider.apply(downloadDataConnector))

        result.availableFilesTable.isDefined mustBe true
        result.pendingFilesTable mustBe None
        result.doesGoodsRecordExist mustBe false
      }
    }

    "when there are pending files" - {
      "should create a FileManagementViewModel with pending files table" in {

        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(downloadDataConnector.getDownloadDataSummary).thenReturn(
          Future.successful(
            Seq(
              DownloadDataSummary(
                "id1",
                "EORI123",
                FileInProgress,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Some(FileInfo("file1.csv", 100, "30"))
              )
            )
          )
        )
        when(downloadDataConnector.getDownloadData).thenReturn(
          Future.successful(
            Seq(
              DownloadData("url1", "file1.csv", 100, Seq())
            )
          )
        )
        when(goodsRecordConnector.getRecords(1, 1))
          .thenReturn(Future.successful(Some(GetRecordsResponse(Seq.empty, defaultPagination))))

        val provider = new FileManagementViewModelProvider(fileManagementTableComponentHelper, goodsRecordConnector)
        val result   = await(provider.apply(downloadDataConnector))

        result.availableFilesTable mustBe None
        result.pendingFilesTable.isDefined mustBe true
        result.doesGoodsRecordExist mustBe false
      }
    }

    "when there are failed files" - {
      "should create a FileManagementViewModel with failed files table" in {

        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(downloadDataConnector.getDownloadDataSummary).thenReturn(
          Future.successful(
            Seq(
              DownloadDataSummary(
                "id1",
                "EORI123",
                FileFailedUnseen,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(5),
                Some(FileInfo("file1.csv", 100, "30"))
              )
            )
          )
        )
        when(downloadDataConnector.getDownloadData).thenReturn(
          Future.successful(
            Seq(
              DownloadData("url1", "file1.csv", 100, Seq())
            )
          )
        )
        when(goodsRecordConnector.getRecords(1, 1))
          .thenReturn(Future.successful(Some(GetRecordsResponse(Seq.empty, defaultPagination))))

        val provider = new FileManagementViewModelProvider(fileManagementTableComponentHelper, goodsRecordConnector)
        val result   = await(provider.apply(downloadDataConnector))

        result.availableFilesTable mustBe None
        result.pendingFilesTable mustBe None
        result.failedFilesTable.isDefined mustBe true
        result.doesGoodsRecordExist mustBe false
      }
    }

    "when there are no files" - {
      "should create a FileManagementViewModel with no files" in {

        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(downloadDataConnector.getDownloadDataSummary).thenReturn(Future.successful(Seq.empty))
        when(downloadDataConnector.getDownloadData).thenReturn(Future.successful(Seq.empty))
        when(goodsRecordConnector.getRecords(1, 1))
          .thenReturn(Future.successful(Some(GetRecordsResponse(Seq.empty, defaultPagination))))

        val provider = new FileManagementViewModelProvider(fileManagementTableComponentHelper, goodsRecordConnector)
        val result   = await(provider.apply(downloadDataConnector))

        result.availableFilesTable mustBe None
        result.pendingFilesTable mustBe None
        result.doesGoodsRecordExist mustBe false
      }
    }
  }
}
