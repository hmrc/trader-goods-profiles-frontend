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

package models.filemanagement

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow

trait FileManagementTable {
  val title: String
  val body: Option[String]
  val headRows: Seq[HeadCell]
  val rows: Seq[Seq[TableRow]]
}

case class AvailableFilesTable()(implicit messages: Messages) extends FileManagementTable {
  // TODO: Add apply in companion object to create table rows from data
  override val title: String = messages("fileManagement.availableFiles.table.title")
  override val body: Option[String] = None
  override val headRows: Seq[HeadCell] =
    Seq(
      messages("fileManagement.availableFiles.table.header1"),
      messages("fileManagement.availableFiles.table.header2"),
      messages("fileManagement.availableFiles.table.header3")
    ).map { content =>
      HeadCell(content = Text(content))
    }
  override val rows: Seq[Seq[TableRow]] = Seq(
    Seq(
      TableRow(
        content = Text("time answer"),
      ),
      TableRow(
        content = Text("expiry answer"),
      ),
      TableRow(
        content = Text("file answer"),
      )
    ),
    Seq(
      TableRow(
        content = Text("time answer"),
      ),
      TableRow(
        content = Text("expiry answer"),
      ),
      TableRow(
        content = Text("file answer"),
      )
    )
  )
}

case class PendingFilesTable()(implicit messages: Messages) extends FileManagementTable {
  // TODO: Add apply in companion object to create table rows from data
  override val title: String = messages("fileManagement.pendingFiles.table.title")
  override val body: Option[String] = Some(messages("fileManagement.pendingFiles.paragraph2"))
  override val headRows: Seq[HeadCell] =
    Seq(
      messages("fileManagement.pendingFiles.table.header1"),
      messages("fileManagement.pendingFiles.table.header2")
    ).map { content =>
      HeadCell(content = Text(content))
    }
  override val rows: Seq[Seq[TableRow]] = Seq(
    Seq(
      TableRow(
        content = Text("time answer"),
      ),
      TableRow(
        content = Text("file answer"),
      )
    ),
    Seq(
      TableRow(
        content = Text("time answer"),
      ),
      TableRow(
        content = Text("file answer"),
      )
    )
  )
}