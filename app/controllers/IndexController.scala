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

import config.FrontendAppConfig
import connectors.{DownloadDataConnector, TraderProfileConnector}
import controllers.actions.IdentifierAction
import models.TraderProfile
import models.requests.IdentifierRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  traderProfileConnector: TraderProfileConnector,
  downloadDataConnector: DownloadDataConnector,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    def checkProfileAndContinue =
      traderProfileConnector.checkTraderProfile.flatMap {
        case true  => eoriChanged(request)
        case false => Future.successful(Redirect(controllers.profile.routes.ProfileSetupController.onPageLoad()))
      }

    if (config.downloadFileEnabled) {
      downloadDataConnector.getEmail.flatMap {
        case Some(_) =>
          checkProfileAndContinue
        case None    =>
          Future.successful(
            Redirect(url"${config.customsEmailUrl}/manage-email-cds/service/trader-goods-profiles".toString)
          )
      }
    } else {
      checkProfileAndContinue
    }
  }

  private def eoriChanged(request: IdentifierRequest[AnyContent])(implicit hc: HeaderCarrier) =
    traderProfileConnector.getTraderProfile(request.eori).map {
      case TraderProfile(_, _, _, _, eoriChanged) if eoriChanged =>
        Redirect(controllers.newUkims.routes.UkimsNumberChangeController.onPageLoad())
      case _                                                     =>
        Redirect(routes.HomePageController.onPageLoad())
    }
}
