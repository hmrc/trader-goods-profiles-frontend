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
import base.TestConstants.testEori
import connectors.TraderProfileConnector
import models.TraderProfile
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EoriCheckActionSpec extends SpecBase with MockitoSugar {

  class Harness(mockTraderProfileConnector: TraderProfileConnector)
      extends EoriCheckActionImpl(mockTraderProfileConnector) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  "Eori Check Action" - {

    "must redirect to home page when eori does not need to be changed" in {

      val profile = TraderProfile(testEori, "UKIMS", None, None, eoriChanged = false)

      val mockTraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(profile)

      val action = new Harness(mockTraderProfileConnector)
      val result =
        action.callFilter(IdentifierRequest(FakeRequest(), "id", testEori, AffinityGroup.Individual)).futureValue
      result mustBe Some(Redirect("/trader-goods-profiles/homepage"))
    }

    "must not redirect when eori does need to be changed" in {

      val profile = TraderProfile(testEori, "UKIMS", None, None, eoriChanged = true)

      val mockTraderProfileConnector = mock[TraderProfileConnector]

      when(mockTraderProfileConnector.getTraderProfile(any())(any())) thenReturn Future.successful(profile)

      val action = new Harness(mockTraderProfileConnector)
      val result =
        action.callFilter(IdentifierRequest(FakeRequest(), "id", testEori, AffinityGroup.Individual)).futureValue
      result mustBe None
    }
  }
}
