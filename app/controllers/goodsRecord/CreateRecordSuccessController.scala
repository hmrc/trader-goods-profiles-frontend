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

import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.*
import models.{DeclarableStatus, Scenario}
import models.router.responses.GetGoodsRecordResponse
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.AutoCategoriseService
import views.html.goodsRecord.{CreateRecordAutoCategorisationSuccessView, CreateRecordSuccessView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateRecordSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  autoCategoriseService: AutoCategoriseService,
  goodsRecordConnector: GoodsRecordConnector,
  defaultView: CreateRecordSuccessView,
  autoCategorisationView: CreateRecordAutoCategorisationSuccessView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      if (recordId.isEmpty) {
        Future.successful(BadRequest("Invalid record ID"))
      } else {
        autoCategoriseService.autoCategoriseRecord(recordId, request.userAnswers).flatMap { _ =>
          goodsRecordConnector.getRecord(recordId).map { record =>
            val scenario = Scenario.fromInt(record.category)
            renderView(recordId, scenario, record)
          }
        }
      }
    }

  private def renderView(recordId: String, scenario: Option[Scenario], record: GetGoodsRecordResponse)(implicit
    request: Request[_],
    messages: Messages
  ): Result =
    scenario match {
      case Some(_) =>
        record.declarable match {
          case DeclarableStatus.ImmiReady | DeclarableStatus.NotReadyForImmi =>
            val tagText = messages(record.declarable.messageKey)
            Ok(autoCategorisationView(recordId, record.declarable == DeclarableStatus.ImmiReady, tagText))
          case DeclarableStatus.NotReadyForUse                               =>
            Ok(defaultView(recordId, scenario))
        }
      case None    =>
        Ok(defaultView(recordId, None))
    }
}
