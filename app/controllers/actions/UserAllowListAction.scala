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
import models.requests.IdentifierRequest
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionRefiner, Result}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAllowListActionImpl @Inject() (
  userAllowListConnector: UserAllowListConnector,
  config: Configuration
)(implicit
  val executionContext: ExecutionContext
) extends UserAllowListAction
    with Logging {

  override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val userAllowListEnabled       = config.get[Boolean]("features.user-allow-list-enabled")

    if (userAllowListEnabled) {
      userAllowListConnector
        .check("private-beta", request.eori)
        .flatMap {
          case false =>
            Future.successful(
              Left(
                Forbidden(
                  Json.toJson(
                    UUID.randomUUID(),
                    "FORBIDDEN",
                    "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
                  )
                )
              )
            )
          case true  => Future.successful(Right(request))
        } recoverWith { case e: Exception =>
        logger.warn(
          s"[UserAllowListAction] - Exception when checking if user was on the allow list",
          e
        )
        Future.failed(e)
      }
    } else {
      Future.successful(Right(request))
    }
  }
}

@ImplementedBy(classOf[UserAllowListActionImpl])
trait UserAllowListAction extends ActionRefiner[IdentifierRequest, IdentifierRequest]
