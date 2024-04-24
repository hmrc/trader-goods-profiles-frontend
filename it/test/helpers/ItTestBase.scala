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

package helpers

import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import play.api.inject.bind
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.Future

trait ItTestBase extends PlaySpec
  with GuiceOneServerPerSuite{

  val appRouteContext: String = "/trader-goods-profiles"
  private val mockAppConfig   = mock[FrontendAppConfig]
  lazy val authConnector: AuthConnector = mock[AuthConnector]

  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector)
      )
  }

  override implicit lazy val app: Application = appBuilder.build()
  private val appConfig                       = app.injector.instanceOf[FrontendAppConfig]
  private val eori                            = Gen.alphaNumStr.sample.get
  private val authFetch                       = Retrievals.internalId and Retrievals.authorisedEnrolments
  private val ourEnrolment: Enrolment         =
    Enrolment(appConfig.tgpEnrolmentIdentifier.key).withIdentifier(appConfig.tgpEnrolmentIdentifier.identifier, eori)
  private val authResult                      = Some("internalId") and Enrolments(Set(ourEnrolment))

  def authorisedUser: OngoingStubbing[Future[Option[String] ~ Enrolments]] = {
    when(authConnector.authorise(any, eqTo(authFetch))(any, any)).thenReturn(
      Future.successful(authResult)
    )
  }

  def noEnrolment: OngoingStubbing[Future[Option[String] ~ Enrolments]] = {
    val authResult = Some("internalId") and Enrolments(Set.empty)
    when(authConnector.authorise(any, eqTo(authFetch))(any, any)).thenReturn(
      Future.successful(authResult)
    )
  }

  def redirectUrl(response: WSResponse): Option[String] = {
    response.header(HeaderNames.LOCATION)
  }
}
