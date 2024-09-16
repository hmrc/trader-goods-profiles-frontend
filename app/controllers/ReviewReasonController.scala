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

import connectors.GoodsRecordConnector
import controllers.actions._
import models.NormalMode
import navigation.Navigator
import pages.ReviewReasonPage

import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.ReviewReasonView

import scala.concurrent.ExecutionContext

class ReviewReasonController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ReviewReasonView,
  goodsRecordConnector: GoodsRecordConnector,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .map { record =>
          record.reviewReason match {
            case Some(reviewReason) if record.toReview => Ok(view(recordId, reviewReason.toLowerCase))
            case _                                     => Redirect(navigator.nextPage(ReviewReasonPage(recordId), NormalMode, request.userAnswers))
          }
        }
        .recover { case _ =>
          navigator.journeyRecovery()
        }
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => Redirect(navigator.nextPage(ReviewReasonPage(recordId), NormalMode, request.userAnswers))
  }

}
