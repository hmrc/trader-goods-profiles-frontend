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

package connectors

import base.TestConstants.testEori
import com.github.tomakehurst.wiremock.client.WireMock._
import models.TraderProfile
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class RouterConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[RouterConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".submitTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = TraderProfile(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/good-profiles/$testEori"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(ok())
      )

      connector.submitTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = TraderProfile(testEori,"1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo("/trader-goods-profiles-router/customs/traders/good-profiles"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(serverError())
      )

      connector.submitTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }
}
