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

import models.{InternalId, UserAnswers}
import repositories.SessionRepository
import cats.data.EitherT
import models.errors.SessionError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionService @Inject() (sessionRepository: SessionRepository)(implicit ec: ExecutionContext) {

  def createUserAnswers(internalId: InternalId): EitherT[Future, SessionError, Unit] =
    EitherT {
      val emptyUserAnswers = UserAnswers(internalId.value)
      sessionRepository
        .set(emptyUserAnswers)
        .map(_ => Right(()))
        .recover { case thr => Left(SessionError.InternalUnexpectedError(thr)) }
    }

  def readUserAnswers(id: InternalId): EitherT[Future, SessionError, Option[UserAnswers]] =
    EitherT {
      sessionRepository
        .get(id)
        .map(Right(_))
        .recover { case thr => Left(SessionError.InternalUnexpectedError(thr)) }
    }

  def updateUserAnswers(userAnswers: UserAnswers): EitherT[Future, SessionError, Unit] =
    EitherT {
      sessionRepository
        .set(userAnswers)
        .map(_ => Right(()))
        .recover { case thr => Left(SessionError.InternalUnexpectedError(thr)) }
    }

}
