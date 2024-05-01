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

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.EnrolmentConfig
import models.requests.AuthorisedRequest
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  private val frontendAppConfig = mock[FrontendAppConfig]
  private val loginUrl          = "loginUrl"
  when(frontendAppConfig.loginUrl).thenReturn(loginUrl)
  when(frontendAppConfig.loginContinueUrl).thenReturn("loginContinueUrl")
  when(frontendAppConfig.tgpEnrolmentIdentifier).thenReturn(EnrolmentConfig("key", "identifier"))

  private val mockParsers = mock[BodyParsers.Default]

  private def block(authRequest: AuthorisedRequest[_]) =
    Future.successful(Results.Ok)

  class Harness(authAction: AuthoriseAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val authAction =
          new AuthoriseActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), frontendAppConfig, mockParsers)

        val result = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) must startWith(loginUrl)
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val authAction =
          new AuthoriseActionImpl(new FakeFailingAuthConnector(new BearerTokenExpired), frontendAppConfig, mockParsers)
        val result     = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) must startWith(loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val authAction =
          new AuthoriseActionImpl(
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            frontendAppConfig,
            mockParsers
          )
        val result     = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val authAction = new AuthoriseActionImpl(
          new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
          frontendAppConfig,
          mockParsers
        )

        val result = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val authAction =
          new AuthoriseActionImpl(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            frontendAppConfig,
            mockParsers
          )

        val result = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val authAction =
          new AuthoriseActionImpl(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            frontendAppConfig,
            mockParsers
          )

        val result = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val authAction =
          new AuthoriseActionImpl(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            frontendAppConfig,
            mockParsers
          )

        val result = await(authAction.invokeBlock(fakeRequest, block))

        result.header.status mustBe SEE_OTHER
        result.header.headers(LOCATION) mustBe routes.UnauthorisedController.onPageLoad.url
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}
