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

import connectors.TraderProfileConnector
import controllers.actions.IdentifierAction
import models.TraderProfile
import models.requests.IdentifierRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    traderProfileConnector.checkTraderProfile(request.eori).flatMap {
      case true =>
        eoriChanged(request)
      case false => Future.successful(Redirect(routes.ProfileSetupController.onPageLoad()))
    }
  }

  private def eoriChanged(request: IdentifierRequest[AnyContent])(implicit hc: HeaderCarrier) = {
    traderProfileConnector.getTraderProfile(request.eori).map {
      case TraderProfile(_, _, _, _, eoriChanged) =>
        if (eoriChanged) {
          Redirect(routes.UkimsNumberChangeController.onPageLoad())
        } else {
          Redirect(routes.HomePageController.onPageLoad())
        }
    }
  }
}
