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
import connectors.UserAllowListConnector
import controllers.routes
import models.EnrolmentConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthActionSpec extends SpecBase with MockitoSugar {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  private val userAllowListConnector = mock[UserAllowListConnector]

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new BearerTokenExpired),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedCdsEnrolmentController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user's EORI number is empty" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeSuccessfulAuthConnector(""),
            userAllowListConnector,
            appConfig,
            bodyParsers
          )
          when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user is on the user-allow-list" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = mock[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeSuccessfulAuthConnector("GB"),
          userAllowListConnector,
          appConfig,
          bodyParsers
        )
        when(appConfig.userAllowListEnabled).thenReturn(true)
        when(appConfig.tgpEnrolmentIdentifier).thenReturn(EnrolmentConfig("HMRC-CUS-ORG", "EORINumber"))
        when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(true))

        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "the user is not on the user-allow-list" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = mock[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeSuccessfulAuthConnector("1234"),
          userAllowListConnector,
          appConfig,
          bodyParsers
        )
        when(appConfig.userAllowListEnabled).thenReturn(true)
        when(appConfig.tgpEnrolmentIdentifier).thenReturn(EnrolmentConfig("HMRC-CUS-ORG", "EORINumber"))
        when(userAllowListConnector.check(any, any)(any)).thenReturn(Future.successful(false))

        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedServiceUserController.onPageLoad().url
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

class FakeSuccessfulAuthConnector @Inject() (eori: String) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = {
    val authResponse = new ~(
      new ~(Some("internalId"), Some(AffinityGroup.Individual)),
      Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", eori)), "")))
    )

    Future.fromTry(Try(authResponse.asInstanceOf[A]))
  }
}
