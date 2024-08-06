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
import connectors.UserAllowListConnector
import connectors.UserAllowListConnector.UnexpectedResponseException
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class UserAllowListActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(mockUserAllowListConnector: UserAllowListConnector, config: Configuration)
      extends UserAllowListActionImpl(mockUserAllowListConnector, config) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  private val connector             = mock[UserAllowListConnector]
  private val appConfig             = mock[Configuration]
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override def afterEach(): Unit = {
    super.afterEach()
    reset(connector)
  }

  "refine action" - {
    "return a None if the user is on the user allow list" in {
      val request = IdentifierRequest(FakeRequest(), "1234", "eori", AffinityGroup.Individual)
      when(connector.check(any, any)(any)).thenReturn(Future.successful(true))
      when(appConfig.get[Boolean]("features.user-allow-list-enabled")).thenReturn(true)

      val harness = new Harness(connector, appConfig)
      val result  = harness.callFilter(request).futureValue

      result mustBe None
    }

    "should redirect a user to the /unauthorised-service-user page if they are not on the user allow list" in {
      val request = IdentifierRequest(FakeRequest(), "1234", "eori", AffinityGroup.Individual)
      when(connector.check(any, any)(any)).thenReturn(Future.successful(false))
      when(appConfig.get[Boolean]("features.user-allow-list-enabled")).thenReturn(true)

      val harness = new Harness(connector, appConfig)
      val result  = harness.callFilter(request).futureValue

      result mustBe Some(Redirect("/trader-goods-profiles/problem/unauthorised-service-user"))
    }

    "should return a None if the userAllowListEnabled feature flag is false" in {
      val request = IdentifierRequest(FakeRequest(), "1234", "eori", AffinityGroup.Individual)
      when(appConfig.get[Boolean]("features.user-allow-list-enabled")).thenReturn(false)

      val harness = new Harness(connector, appConfig)
      val result  = harness.callFilter(request).futureValue

      result mustBe None
      verifyNoInteractions(connector)
    }

    "should redirect a user to the /unauthorised-service-user page if the user-allow-list service throws an error" in {
      val request = IdentifierRequest(FakeRequest(), "1234", "eori", AffinityGroup.Individual)
      when(connector.check(any, any)(any)).thenReturn(Future.failed(UnexpectedResponseException(BAD_REQUEST)))
      when(appConfig.get[Boolean]("features.user-allow-list-enabled")).thenReturn(true)

      val harness = new Harness(connector, appConfig)
      val result  = harness.callFilter(request).futureValue

      result mustBe Some(Redirect("/trader-goods-profiles/problem/unauthorised-service-user"))
    }

  }
}
