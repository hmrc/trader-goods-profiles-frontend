/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import base.SpecBase
import controllers.actions.{FakeAuthoriseAction, FakeSessionRequestAction}
import models.{MaintainProfileAnswers, UkimsNumber, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import repositories.SessionRepository

import scala.concurrent.Future

class KeepAliveControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val sessionRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(sessionRepository)
  }

  "keepAlive" - {

    "when the user has answered some questions" - {

      "must keep the answers alive and return OK" in {

        val userAnswers =
          UserAnswers("idWithAnswers", MaintainProfileAnswers(ukimsNumber = Some(UkimsNumber("testUkims"))))

        val keepAliveControllerWithData = new KeepAliveController(
          messageComponentControllers,
          new FakeAuthoriseAction(defaultBodyParser),
          new FakeSessionRequestAction(userAnswers),
          sessionRepository
        )

        when(sessionRepository.keepAlive(any())) thenReturn Future.successful(true)

        val result = keepAliveControllerWithData.keepAlive()(fakeRequest)

        status(result) mustEqual OK
        verify(sessionRepository, times(1)).keepAlive(userAnswers.id)

      }
    }

    "when the user has not answered any questions" - {

      "must return OK" in {

        val keepAliveControllerWithData = new KeepAliveController(
          messageComponentControllers,
          new FakeAuthoriseAction(defaultBodyParser),
          new FakeSessionRequestAction(emptyUserAnswers),
          sessionRepository
        )

        when(sessionRepository.keepAlive(any())) thenReturn Future.successful(true)

        val result = keepAliveControllerWithData.keepAlive()(fakeRequest)

        status(result) mustEqual OK
        verify(sessionRepository, never).keepAlive(emptyUserAnswers.id)

      }
    }
  }
}
