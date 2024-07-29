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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.GoodsRecordsLoadingView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GoodsRecordsLoadingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsRecordsLoadingView,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(continueUrl: Option[RedirectUrl] = None): Action[AnyContent] =
    identify.async { implicit request =>
      goodsRecordConnector.getRecordsSummary(request.eori).map { recordsSummary =>
        recordsSummary.currentUpdate.map { update =>
          val recordsStored  = update.recordsStored
          val recordsToStore = update.recordsToStore
          Ok(view(recordsStored, recordsToStore))
        }.getOrElse {
          continueUrl
            .flatMap(_.getEither(OnlyRelative).toOption)
            .map(safeRedirect => Redirect(safeRedirect.url))
            .getOrElse(Redirect(routes.HomePageController.onPageLoad().url))
        }
      }
    }
}
