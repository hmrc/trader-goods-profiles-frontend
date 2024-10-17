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

import models.filemanagement._
import play.api.i18n.Messages
import javax.inject.Inject


case class FileManagementViewModel(
                                    availableFilesTable: Option[AvailableFilesTable],
                                    pendingFilesTable: Option[PendingFilesTable]
                                  )(implicit messages: Messages) {

  val isFiles: Boolean = availableFilesTable.isDefined || pendingFilesTable.isDefined

  val title: String = messages("fileManagement.title")
  val heading: String = messages("fileManagement.heading")

  val paragraph1: String = if (isFiles) messages("fileManagement.files.paragraph1") else messages("fileManagement.noFiles.paragraph1")

  val tgpRecordsLink: String = messages("fileManagement.requestRecord.linkText")
  val goBackHomeLink: String = messages("site.goBackToHomePage")
}

object FileManagementViewModel {
  class FileManagementViewModelProvider @Inject() {
    def apply()(implicit messages: Messages): FileManagementViewModel = {
      // TODO - Implement method to sort data into correct case models for table rows
      new FileManagementViewModel(Some(AvailableFilesTable()), Some(PendingFilesTable()))
    }
  }
}