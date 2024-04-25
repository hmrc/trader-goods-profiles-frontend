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
import models.{Eori, InternalId}
import models.requests.{AuthorisedRequest, DataRequest}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import services.SessionService
import org.mockito.Mockito.when
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionRequestActionSpec extends SpecBase with MockitoSugar {
  class Harness(sessionService: SessionService) extends SessionRequestActionImpl(sessionService) {
    def callRefine[A](request: AuthorisedRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

//  "Session Request Action" - {
//
//    "when there is no data in the cache" - {
//
//      "must set userAnswers to 'None' in the request" in {
//
//        val sessionService = mock[SessionService]
//        when(sessionService.getUserAnswers(InternalId("id"))) thenReturn Future(Right(None))
//        val action         = new Harness(sessionService)
//
//        val result = action.callRefine(AuthorisedRequest(FakeRequest(), InternalId("id"), Eori("eori"))).futureValue
//
//        result.leftSide.must not be defined
//      }
//    }
//
//    "when there is data in the cache" - {
//
//      "must build a userAnswers object and add it to the request" in {
//
//        val sessionRepository = mock[SessionRepository]
//        when(sessionRepository.get("id")) thenReturn Future(Some(UserAnswers("id")))
//        val action            = new Harness(sessionRepository)
//
//        val result = action.callTransform(AuthorisedRequest(FakeRequest(), InternalId("id"), Eori("eori"))).futureValue
//
//        result.userAnswers mustBe defined
//      }
//    }
//  }
}
