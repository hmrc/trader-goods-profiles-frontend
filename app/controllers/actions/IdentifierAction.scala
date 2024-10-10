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
import connectors.UserAllowListConnector
import connectors.UserAllowListConnector.UserNotAllowedException
import controllers.routes
import models.requests.IdentifierRequest
import org.apache.pekko.Done
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  userAllowListConnector: UserAllowListConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val predicates =
      Enrolment(config.tgpEnrolmentIdentifier.key) and (AffinityGroup.Organisation or AffinityGroup.Individual)

    authorised(predicates)
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.authorisedEnrolments) {
        case Some(internalId) ~ Some(affinityGroup) ~ authorisedEnrolments =>
          authorisedEnrolments
            .getEnrolment(config.tgpEnrolmentIdentifier.key)
            .flatMap(_.getIdentifier(config.tgpEnrolmentIdentifier.identifier)) match {
            case Some(enrolment) if !enrolment.value.isBlank =>
              checkUserAllowList(enrolment.value)(hc).flatMap { _ =>
                block(IdentifierRequest(request, internalId, enrolment.value, affinityGroup))
              }
            case Some(enrolment) if enrolment.value.isBlank  =>
              throw InternalError("EORI is empty")
            case _                                           =>
              throw InsufficientEnrolments("Unable to retrieve Enrolment")
          }
        // case _ => throw InternalError("Internal Error") // TODO: What should we do when the other cases are not hit? What would be an acceptable error message?
      } recover {
      case _: UserNotAllowedException        =>
        logger.info("trader is not on user-allow-list redirecting to UnauthorisedServiceController")
        Redirect(routes.UnauthorisedServiceUserController.onPageLoad())
      case _: NoActiveSession                =>
        logger.info(s"No Active Session. Redirect to $config.loginContinueUrl")
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments         =>
        logger.info(
          "Authorisation failure: No enrolments found for CDS. Redirecting to UnauthorisedCdsEnrolmentController"
        )
        Redirect(routes.UnauthorisedCdsEnrolmentController.onPageLoad())
      case exception: AuthorisationException =>
        logger.info(f"Authorisation failure: ${exception.reason}. Redirecting to UnauthorisedController")
        Redirect(routes.UnauthorisedController.onPageLoad)
    }
  }

  private def checkUserAllowList(eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    if (config.userAllowListEnabled) {
      userAllowListConnector
        .check("private-beta", eori)
        .map {
          case false =>
            logger.info("user not on allow list")
            throw UserNotAllowedException()
          case true  => Done
        } recover { case _ =>
        throw UserNotAllowedException()
      }
    } else {
      logger.info("user allow list feature flag is disabled, always returning successfully")
      Future.successful(Done)
    }

}
