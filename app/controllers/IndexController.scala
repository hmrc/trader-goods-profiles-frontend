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
import connectors.{DownloadDataConnector, GoodsRecordConnector, TraderProfileConnector}
import controllers.actions.IdentifierAction
import models.requests.IdentifierRequest
import models.{RecordsSummary, TraderProfile}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  traderProfileConnector: TraderProfileConnector,
  downloadDataConnector: DownloadDataConnector,
  goodsRecordConnector: GoodsRecordConnector,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    goodsRecordConnector.getRecordsSummary.flatMap(recordsUpdateCheck(_))
  }

  private def recordsUpdateCheck(
    recordsSummary: RecordsSummary
  )(implicit request: IdentifierRequest[AnyContent]): Future[Result] =
    recordsSummary.currentUpdate.map(_.totalRecords) match {
      case Some(totalRecords) if totalRecords > config.redirectLoadThreshold =>
        Future.successful(
          Redirect(
            controllers.goodsProfile.routes.GoodsRecordsLoadingController.onPageLoad(
              Some(RedirectUrl(controllers.routes.IndexController.onPageLoad().url))
            )
          )
        )
      case _                                                                 => verifiedEmailRedirectCheck
    }

  private def checkProfileAndContinue(implicit hc: HeaderCarrier): Future[Result] =
    traderProfileConnector.checkTraderProfile.flatMap {
      case true  => eoriChanged
      case false => Future.successful(Redirect(controllers.profile.routes.ProfileSetupController.onPageLoad()))
    }

  private def verifiedEmailRedirectCheck(implicit request: IdentifierRequest[AnyContent]) =
    if (config.downloadFileEnabled && request.credentialRole.contains(User)) {
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

  private def eoriChanged(implicit hc: HeaderCarrier) =
    traderProfileConnector.getTraderProfile.map {
      case TraderProfile(_, _, _, _, eoriChanged) if eoriChanged =>
        Redirect(controllers.newUkims.routes.UkimsNumberChangeController.onPageLoad())
      case _                                                     =>
        Redirect(routes.HomePageController.onPageLoad())
    }
}
