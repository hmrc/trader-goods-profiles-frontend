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
import cats.data.EitherT
import controllers.routes
import models.errors.SessionError
import models.requests.{AuthorisedRequest, DataRequest}
import models.{Eori, InternalId, TraderGoodsProfile, UserAnswers}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService

import scala.concurrent.Future

class SessionRequestActionSpec extends SpecBase with MockitoSugar {
  class Harness(sessionService: SessionService) extends SessionRequestActionImpl(sessionService)(ec) {
    def callRefine[A](
      request: AuthorisedRequest[A]
    ): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "Session Request Action" - {
    val internalId          = InternalId("id")
    val eori                = Eori("eori")
    val authRequest         = AuthorisedRequest(FakeRequest(), internalId, eori)
    val sessionService      = mock[SessionService]
    val repositoryThrowable = new Throwable("There was an error with sessionRepository")
    val userAnswers         = UserAnswers(internalId.value, TraderGoodsProfile())

    "when it is a new user" - {

      "must create a new user answers and redirect to ProfileSetupController" in {
        when(sessionService.readUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Option[UserAnswers]](
          Future.successful(Right(None))
        )
        when(sessionService.createUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Unit](
          Future.successful(Right(()))
        )

        val action = new Harness(sessionService)
        val result = await(action.callRefine(authRequest))

        result mustBe Left(Redirect(routes.ProfileSetupController.onPageLoad))
      }
      "redirects if unexpected error when creating answers" in {
        when(sessionService.readUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Option[UserAnswers]](
          Future.successful(Right(None))
        )
        when(sessionService.createUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Unit](
          Future.successful(Left(SessionError.InternalUnexpectedError(repositoryThrowable)))
        )

        val action = new Harness(sessionService)
        val result = await(action.callRefine(authRequest))

        result mustBe Left(Redirect(routes.DummyController.onPageLoad))

      }
    }

    "when it is a not a new user" - {

      "must get user answers" in {
        when(sessionService.readUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Option[UserAnswers]](
          Future.successful(Right(Some(userAnswers)))
        )

        val action = new Harness(sessionService)
        val result = await(action.callRefine(authRequest))

        result mustBe Right(DataRequest(authRequest, internalId, userAnswers, eori))
      }

      "redirects if unexpected error when getting answers" in {

        when(sessionService.readUserAnswers(internalId)) thenReturn EitherT[Future, SessionError, Option[UserAnswers]](
          Future.successful(Left(SessionError.InternalUnexpectedError(repositoryThrowable)))
        )

        val action = new Harness(sessionService)
        val result = await(action.callRefine(authRequest))

        result mustBe Left(Redirect(routes.DummyController.onPageLoad))

      }

    }
  }
}
