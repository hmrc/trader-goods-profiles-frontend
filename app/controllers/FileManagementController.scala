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

import connectors.DownloadDataConnector
import controllers.actions._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.FileManagementViewModel.FileManagementViewModelProvider
import views.html.FileManagementView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FileManagementController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  viewModelProvider: FileManagementViewModelProvider,
  val controllerComponents: MessagesControllerComponents,
  view: FileManagementView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
//    downloadDataConnector.getDownloadDataSummary(request.eori).map { downloadData => TODO: Pull out data as needed when TGP-2730 is complete
//      viewModel(downloadDataConnector).map { viewModel =>
        Ok(view(viewModelProvider()))
      }
}
