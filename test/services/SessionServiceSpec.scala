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

package services

import base.SpecBase
import cats.data.EitherT
import models.{InternalId, UserAnswers}
import models.errors.SessionError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}
class SessionServiceSpec extends SpecBase {

  "Session Service" - {

    val mockSessionRepository = mock[SessionRepository]
    val sessionService        = new SessionService(mockSessionRepository)
    val id                    = InternalId("id")
    val repositoryThrowable   = new Throwable("There was an error with sessionRepository")

    "createUserAnswers should create answers if sessionRepository does not fail" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.successful(true)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.createUserAnswers(id)

      withClue("Session service should not fail to create answers") {
        result.value.futureValue mustBe Right()
      }

    }

    "createUserAnswers should not create answers if sessionRepository fails" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.createUserAnswers(id)

      withClue("Session service should fail to create answers but doesn't") {
        result.value.futureValue shouldBe a[Left[_, Unit]]
      }
    }

    "readUserAnswers should get answers if sessionRepository does not fail" in {
      when(
        mockSessionRepository.get(any())
      ).thenReturn(
        Future.successful(Some(emptyUserAnswers))
      )

      val result: EitherT[Future, SessionError, Option[UserAnswers]] = sessionService.readUserAnswers(id)

      withClue("Session service should get answers and not error") {
        result.value.futureValue shouldBe a[Right[_, Option[UserAnswers]]]
      }
    }

    "readUserAnswers should not get answers if sessionRepository fails" in {
      when(
        mockSessionRepository.get(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Option[UserAnswers]] = sessionService.readUserAnswers(id)

      withClue("Session service should not get answers and should fail") {
        result.value.futureValue shouldBe a[Left[_, Option[UserAnswers]]]
      }
    }

    "updateUserAnswers should update answers if sessionRepository does not fail" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.successful(true)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.updateUserAnswers(emptyUserAnswers)
      result.value.futureValue match {
        case Left(sessionError) => fail("Session service should not fail to update answers")
        case Right(unit)        => succeed
      }

      withClue("Session service should not fail to update answers") {
        result.value.futureValue mustBe Right()
      }
    }

    "updateUserAnswers should not update answers if sessionRepository fails" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.updateUserAnswers(emptyUserAnswers)

      withClue("Session service should fail to update answers but doesn't") {
        result.value.futureValue shouldBe a[Left[_, Unit]]
      }
    }

  }

}
