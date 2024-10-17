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

package models

import base.SpecBase
import models.filemanagement._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, Text}

class FileManagementTableSpec extends SpecBase {

  "FileManagementTable" - {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    implicit val message: Messages = messages(application)

    "AvailableFilesTable" - {
      val table = AvailableFilesTable()

      "must return correct caption" in {
        table.caption mustEqual "TGP records files"
      }

      "must return correct body" in {
        table.body mustBe None
      }

      "must return correct headRows" in {
        table.headRows mustBe Seq(
          HeadCell(content = Text("Date and time requested")),
          HeadCell(content = Text("Expiry date")),
          HeadCell(content = Text("File"))
        )
      }
    }

    "PendingFilesTable" - {
      val table = PendingFilesTable()

      "must return correct caption" in {
        table.caption mustEqual "Pending files"
      }

      "must return correct body" in {
        table.body mustBe Some(
          "These are requests you've made but the file is not ready yet. We'll email you when the file is ready."
        )
      }

      "must return correct headRows" in {
        table.headRows mustBe Seq(
          HeadCell(content = Text("Date and time requested")),
          HeadCell(content = Text("File"))
        )
      }
    }

  }
}
