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

import cats.data.EitherT
import controllers.actions._
import models.{InternalId, UserAnswers}
import models.errors.SessionError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.{stubMessagesApi, stubMessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with GuiceOneAppPerSuite {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val userAnswersId: String = "id"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit val messagesApi: MessagesApi = stubMessagesApi()
  val messages: Messages                = messagesApi.preferred(fakeRequest)

  lazy val messageComponentControllers: MessagesControllerComponents = stubMessagesControllerComponents()

  val defaultBodyParser: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  val sessionRequest = new FakeSessionRequestAction(emptyUserAnswers)

  val sessionService: SessionService = mock[SessionService]

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
