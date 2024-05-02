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

package base

import models.UserAnswers
import org.scalatest.concurrent.ScalaFutures
import cats.data.EitherT
import controllers.actions._
import models.errors.SessionError
import models.requests.{AuthorisedRequest, DataRequest}
import models.{InternalId, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.{stubMessagesApi, stubMessagesControllerComponents}

import scala.concurrent.ExecutionContext
import play.api.mvc.{ActionRefiner, AnyContentAsEmpty, PlayBodyParsers}
import services.SessionService

import scala.concurrent.Future

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with GuiceOneAppPerSuite {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val userAnswersId: String = "id"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit val messagesApi: MessagesApi = stubMessagesApi()
  val messages: Messages                = messagesApi.preferred(fakeRequest)

  lazy val messageComponentControllers: MessagesControllerComponents = stubMessagesControllerComponents()

  val defaultBodyParser: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val sessionRequest = new FakeSessionRequestAction(emptyUserAnswers)

  val sessionService = mock[SessionService]

  when(sessionService.readUserAnswers(any[InternalId])) thenReturn EitherT[Future, SessionError, Option[UserAnswers]](
    Future.successful(Right(None))
  )

  when(sessionService.createUserAnswers(any[InternalId])) thenReturn EitherT[Future, SessionError, Unit](
    Future.successful(Right(()))
  )

  when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
    Future.successful(Right(()))
  )

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

}
