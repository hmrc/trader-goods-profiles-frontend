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
import models.NormalMode
import navigation.Navigator
import pages.RequestDataPage

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RequestDataView

import scala.concurrent.ExecutionContext

class RequestDataController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: RequestDataView,
  navigator: Navigator,
  downloadDataConnector: DownloadDataConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      //TODO get this email from the user
      Ok(view("placeholder@email.com"))
  }

  def onSubmit(email: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      //TODO send an email to the user
      downloadDataConnector.requestDownloadData(request.eori).map {
        case true  => Redirect(navigator.nextPage(RequestDataPage, NormalMode, request.userAnswers))
        //TODO implement behaviour if request fails
        case false => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
