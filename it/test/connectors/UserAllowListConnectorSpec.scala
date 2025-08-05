/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.CheckRequest
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.AUTHORIZATION
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import scala.concurrent.ExecutionContext

class UserAllowListConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.user-allow-list.port" -> wireMockPort,
      "internal-auth.token"                        -> "token"
    )
    .build()

  private lazy val connector = app.injector.instanceOf[UserAllowListConnector]

  "UserAllowListConnector#check" - {

    val feature = "private-beta"
    val url     = s"/user-allow-list/trader-goods-profiles/$feature/check"
    val request = CheckRequest("value")

    "returns true when the server responds OK (200)" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader(AUTHORIZATION, equalTo("token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(OK))
      )

      connector.check(feature, request.value).futureValue mustBe true
    }

    "returns false when the server responds NOT_FOUND (404)" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader(AUTHORIZATION, equalTo("token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      connector.check(feature, request.value).futureValue mustBe false
    }

    "fails with UnexpectedResponseException for any other status" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader(AUTHORIZATION, equalTo("token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val exception = connector.check(feature, request.value).failed.futureValue

      exception match {
        case e: UserAllowListConnector.UnexpectedResponseException =>
          e.status mustBe INTERNAL_SERVER_ERROR

          e.getMessage must include("Unexpected status: 500")

        case _ =>
          fail("Expected UnexpectedResponseException")
      }
    }


    "fails if connection is interrupted" in {
      wireMockServer.stubFor(
        post(urlEqualTo(url))
          .withHeader(AUTHORIZATION, equalTo("token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.check(feature, request.value).failed.futureValue
    }
    
  }
}
