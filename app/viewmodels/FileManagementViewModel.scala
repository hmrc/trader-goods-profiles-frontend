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

package viewmodels

import models.DownloadDataSummary
import play.api.i18n.Messages

case class FileManagementViewModel(
                                    availableFilesTableRows: Option[String],
                                    pendingFilesTableRows: Option[String]
                                  )(implicit messages: Messages) {

  val isFiles: Boolean = availableFilesTableRows.isDefined || pendingFilesTableRows.isDefined

  val title: String = messages("fileManagement.title")
  val heading: String = messages("fileManagement.heading")

  val paragraph1: String = if(isFiles) messages("fileManagement.files.paragraph1") else messages("fileManagement.noFiles.paragraph1")
//  val pendingFilesParagraph2: String = messages("fileManagement.pendingFiles.paragraph2") TODO Add this as optional body inside of a case class for handling table rows (AvailableFilesTable + PendingFileTable< FileManagementTable)

  val tgpRecordsLink: String = messages("fileManagement.tgpRecordsLink")
  val goBackHomeLink: String = messages("site.goBackToHomePage")
}

object FileManagementViewModel {

  def apply(
             downloadData: Option[Seq[DownloadDataSummary]], // Assuming we will be returned a model which contains the status of each file
           )(implicit messagesL Messages): FileManagementViewModel = {

    // TODO - Implement this method to sort data into correct case models for table rows

    new FileManagementViewModel(Some(""), Some(""))
  }
}