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

trait SessionService {
  def getUserAnswers(id: InternalId): EitherT[Future, SessionError, Option[UserAnswers]]
  def setUserAnswers(userAnswers: UserAnswers): EitherT[Future, SessionError, UserAnswers]
}

class SessionServiceImpl @Inject() (sessionRepository: SessionRepository)(implicit ec: ExecutionContext)
    extends SessionService {

  def setUserAnswers(userAnswers: UserAnswers): EitherT[Future, SessionError, UserAnswers] =
    EitherT {
      sessionRepository
        .set(userAnswers)
        .map(Right(_))
        .recover { case thr => Left(SessionError.InternalUnexpectedError(thr)) }
    }

  def getUserAnswers(id: InternalId): EitherT[Future, SessionError, Option[UserAnswers]] =
    EitherT {
      sessionRepository
        .get(id)
        .map(Right(_))
        .recover { case thr => Left(SessionError.InternalUnexpectedError(thr)) }
    }

}
