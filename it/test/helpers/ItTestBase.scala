package helpers

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import play.api.inject.bind
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.Future

trait ItTestBase extends PlaySpec
  with GuiceOneServerPerSuite{

  val appRouteContext: String = "/trader-goods-profiles"

  lazy val authConnector: AuthConnector = mock[AuthConnector]

  def appBuilder: GuiceApplicationBuilder = {

    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector)
      )
  }

  override implicit lazy val app: Application = appBuilder.build()

  private val authFetch = Retrievals.internalId and Retrievals.allEnrolments
  private val ourEnrolment: Enrolment = Enrolment("HMRC-CUS-ORG").withIdentifier("fake-identifier", "lasfskjfsdf")
  private val authResult = Some("internalId") and Enrolments(Set(ourEnrolment))

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



}
