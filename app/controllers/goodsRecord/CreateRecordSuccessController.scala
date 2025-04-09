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

package controllers.goodsRecord

import controllers.BaseController
import controllers.actions.*
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCategoriseService
import views.html.goodsRecord.CreateRecordSuccessView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateRecordSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  autoCategoriseService: AutoCategoriseService,
  view: CreateRecordSuccessView
)(implicit ec: ExecutionContext) extends BaseController {

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      autoCategoriseService.autoCategoriseRecord(recordId, request.userAnswers).map {
        autoCategorisedScenario =>
          Ok(view(recordId, autoCategorisedScenario))
      }
    } // TODO Probably want to recover on this if autoCategorise future fails, e.g show default content probably just Ok(view(recordId, None))
    
    // TODO - Will want to remove the link to categorise a good if we've automatically categorised
}
