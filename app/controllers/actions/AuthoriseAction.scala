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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.{Eori, InternalId}
import models.requests.AuthorisedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.auth.core.retrieve.~
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging

trait AuthoriseAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with ActionFunction[Request, AuthorisedRequest]

class AuthoriseActionImpl @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthoriseAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId and Retrievals.allEnrolments) { case Some(internalId) ~ enrolments =>
      val enrolment: Option[Enrolment] = enrolments.getEnrolment(config.tgpEnrolmentIdentifier.key)
      val tgpEnrolment                 = enrolment.flatMap(value => value.getIdentifier(config.tgpEnrolmentIdentifier.identifier))
      tgpEnrolment match {
        case Some(enrolment) => block(AuthorisedRequest(request, InternalId(internalId), Eori(enrolment.value)))
        case None            => throw InsufficientEnrolments("Unable to retrieve Enrolment")
      }
    } recover {
      case _: NoActiveSession        =>
        logger.info(s"NoActiveSession. Redirect to $config.loginContinueUrl")
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        logger.info("AuthorisationException occurred. Redirect to UnauthorisedController")
        Redirect(routes.UnauthorisedController.onPageLoad)
    }
  }
}
