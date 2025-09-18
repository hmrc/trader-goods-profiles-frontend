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
import models.requests.IdentifierRequest
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
      .retrieve(
        Retrievals.internalId and Retrievals.affinityGroup and Retrievals.credentialRole and Retrievals.authorisedEnrolments
      ) {
        case Some(internalId) ~ Some(affinityGroup) ~ credentialRole ~ authorisedEnrolments =>
          handleEnrolments(internalId, affinityGroup, credentialRole, authorisedEnrolments, request, block)
        case _                                                                              =>
          throw InternalError("Undefined authorisation error")
      } recover handleAuthorisationFailures
  }

  private def handleEnrolments[A](
    internalId: String,
    affinityGroup: AffinityGroup,
    credentialRole: Option[CredentialRole],
    authorisedEnrolments: Enrolments,
    request: Request[A],
    block: IdentifierRequest[A] => Future[Result]
  ): Future[Result] =
    authorisedEnrolments
      .getEnrolment(config.tgpEnrolmentIdentifier.key)
      .flatMap(_.getIdentifier(config.tgpEnrolmentIdentifier.identifier)) match {
      case Some(enrolment) if enrolment.value.nonEmpty =>
        block(IdentifierRequest(request, internalId, enrolment.value, affinityGroup, credentialRole))
      case Some(_)                                     =>
        throw InternalError("EORI is empty")
      case None                                        =>
        throw InsufficientEnrolments("Unable to retrieve Enrolment")
    }

  private def handleAuthorisationFailures: PartialFunction[Throwable, Result] = {
    case _: NoActiveSession                =>
      logger.info(s"No Active Session. Redirecting to ${config.loginContinueUrl}.")
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: InsufficientEnrolments         =>
      logger.info(
        "Authorisation failure: No enrolments found for CDS. Redirecting to UnauthorisedCdsEnrolmentController."
      )
      Redirect(config.registerTGPURL)
    case exception: AuthorisationException =>
      logger.info(s"Authorisation failure: ${exception.reason}. Redirecting to UnauthorisedCdsEnrolmentController.")
      Redirect(config.registerTGPURL)
  }

}
