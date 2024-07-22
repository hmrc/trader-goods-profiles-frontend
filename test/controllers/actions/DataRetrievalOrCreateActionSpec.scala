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
import base.TestConstants.{testEori, userAnswersId}
import models.UserAnswers
import models.requests.{DataRequest, IdentifierRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalOrCreateActionSpec extends SpecBase with MockitoSugar {

  class Harness(sessionRepository: SessionRepository) extends DataRetrievalOrCreateActionImpl(sessionRepository) {
    def callTransform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must create userAnswers in the request" in {
        val sessionRepository = mock[SessionRepository]

        when(sessionRepository.get(any())) thenReturn Future(None)

        val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(sessionRepository.set(captor.capture())).thenReturn(Future.successful(true))

        val action = new Harness(sessionRepository)

        action
          .callTransform(IdentifierRequest(FakeRequest(), userAnswersId, testEori, AffinityGroup.Individual))
          .futureValue

        verify(sessionRepository).set(any())

        val savedAnswers = captor.getValue
        savedAnswers.id mustEqual userAnswersId
        savedAnswers.data mustEqual Json.obj()
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        val sessionRepository = mock[SessionRepository]
        val answers           = emptyUserAnswers

        when(sessionRepository.get(userAnswersId)) thenReturn Future(Some(answers))
        val action = new Harness(sessionRepository)

        val result = action
          .callTransform(IdentifierRequest(FakeRequest(), userAnswersId, testEori, AffinityGroup.Individual))
          .futureValue

        verify(sessionRepository, never()).set(any())

        result.userAnswers mustBe answers
      }
    }
  }
}
