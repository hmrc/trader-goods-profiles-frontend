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
import generators.Generators
import models.filemanagement.{AvailableFilesTable, PendingFilesTable}
import org.scalacheck.Gen
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.baseApplicationBuilder.injector

class FileManagementViewModelSpec extends SpecBase with Generators {

  "FileManagementViewModel" - {

    def fakeRequest: FakeRequest[AnyContent] = FakeRequest("", "")
    def messagesApi: MessagesApi             = injector.instanceOf[MessagesApi]
    implicit def messages: Messages          = messagesApi.preferred(fakeRequest)

    val viewModelAvailableFiles = FileManagementViewModel(Some(AvailableFilesTable()), None)
    val viewModelPendingFiles   = FileManagementViewModel(None, Some(PendingFilesTable()))
    val viewModelNoFiles        = FileManagementViewModel(None, None)
    val viewModelAllFiles       = FileManagementViewModel(Some(AvailableFilesTable()), Some(PendingFilesTable()))
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

  }
}
