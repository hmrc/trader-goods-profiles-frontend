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

package controllers.actions

import com.google.inject.ImplementedBy
import connectors.UserAllowListConnector
import controllers.routes
import models.requests.IdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAllowListActionImpl @Inject() (
  userAllowListConnector: UserAllowListConnector,
  config: Configuration
)(implicit
  val executionContext: ExecutionContext
) extends UserAllowListAction
    with Logging {

  override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val userAllowListEnabled       = config.get[Boolean]("features.user-allow-list-enabled")

    if (userAllowListEnabled) {
      userAllowListConnector
        .check("private-beta", request.eori)
        .map {
          case false =>
            logger.info(s"trader with eori: ${request.eori} does not have access to TGP")
            Some(Redirect(routes.UnauthorisedController.onPageLoad))
          case true  => None
        }
    } else {
      logger.info("user allow list feature flag is disabled, always returning successfully")
      Future.successful(None)
    }
  }
}

@ImplementedBy(classOf[UserAllowListActionImpl])
trait UserAllowListAction extends ActionFilter[IdentifierRequest]
