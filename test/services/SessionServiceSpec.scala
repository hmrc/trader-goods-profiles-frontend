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

    implicit val ec: ExecutionContext = ExecutionContext.global;
    val mockSessionRepository         = mock[SessionRepository]
    val sessionService                = new SessionService(mockSessionRepository)
    val id = InternalId("id")
    val repositoryThrowable = new Throwable("There was an error with sessionRepository")

    "createUserAnswers should create answers if sessionRepository does not fail" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.successful(true)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.createUserAnswers(id)
      result.value.futureValue match {
        case Left(sessionError) => fail("Session service should not fail to create answers")
        case Right(unit) => succeed
      }
    }

    "createUserAnswers should not create answers if sessionRepository fails" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.createUserAnswers(id)
      result.value.futureValue match {
        case Left(sessionError) => succeed
        case Right(unit) => fail("Session service should fail to create answers but doesn't")
      }
    }

    "readUserAnswers should get answers if sessionRepository does not fail" in {
      when(
        mockSessionRepository.get(any())
      ).thenReturn(
        Future.successful(Some(emptyUserAnswers))
      )

      val result: EitherT[Future, SessionError, Option[UserAnswers]] = sessionService.readUserAnswers(id)
      result.value.futureValue match {
        case Left(sessionError) => fail("Session service should get answers and not error")
        case Right(Some(answers)) => {
          answers.id shouldEqual emptyUserAnswers.id
          answers.traderGoodsProfile shouldEqual emptyUserAnswers.traderGoodsProfile
        }
      }
    }

    "readUserAnswers should not get answers if sessionRepository fails" in {
      when(
        mockSessionRepository.get(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Option[UserAnswers]] = sessionService.readUserAnswers(id)
      result.value.futureValue match {
        case Left(sessionError) => succeed
        case Right(Some(answers)) => fail("Session service should not get answers and should fail")
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
        case Right(unit) => succeed
      }
    }

    "updateUserAnswers should not update answers if sessionRepository fails" in {
      when(
        mockSessionRepository.set(any())
      ).thenReturn(
        Future.failed(repositoryThrowable)
      )

      val result: EitherT[Future, SessionError, Unit] = sessionService.updateUserAnswers(emptyUserAnswers)
      result.value.futureValue match {
        case Left(sessionError) => succeed
        case Right(unit) => fail("Session service should fail to update answers but doesn't")
      }
    }

  }

}
