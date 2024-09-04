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
import com.github.tomakehurst.wiremock.client.WireMock.{ok, _}
import models.{HistoricProfileData, TraderProfile}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import testModels.DataStoreProfile
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class TraderProfileConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[TraderProfileConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "submitTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = TraderProfile(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/profile"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(ok())
      )

      connector.submitTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = TraderProfile(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/profile"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(serverError())
      )

      connector.submitTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }

  "getTraderProfile" - {

    "must get a trader profile" in {

      val dataStoreTraderProfile = DataStoreProfile(testEori, testEori, "1", Some("2"), None)
      val traderProfile          = TraderProfile(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-data-store/customs/traders/goods-profiles/$testEori"))
          .willReturn(ok().withBody(Json.toJson(dataStoreTraderProfile).toString))
      )

      connector.getTraderProfile(testEori).futureValue mustBe traderProfile
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-data-store/customs/traders/goods-profiles/$testEori"))
          .willReturn(serverError())
      )

      connector.getTraderProfile(testEori).failed.futureValue
    }
  }

  "checkTraderProfile" - {

    "must return true if present" in {

      wireMockServer.stubFor(
        head(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/profile"))
          .willReturn(ok())
      )

      connector.checkTraderProfile(testEori).futureValue mustBe true
    }

    "must return false if not present" in {

      wireMockServer.stubFor(
        head(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/profile"))
          .willReturn(notFound())
      )

      connector.checkTraderProfile(testEori).futureValue mustBe false
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        head(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/profile"))
          .willReturn(serverError())
      )

      connector.checkTraderProfile(testEori).failed.futureValue
    }
  }

  "getHistoricProfileData" - {

    lazy val appWithRouterConfig: Application =
      new GuiceApplicationBuilder()
        .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
        .build()

    lazy val connectorWithRouterConfig = appWithRouterConfig.injector.instanceOf[TraderProfileConnector]

    "must return a Some of profile data if the request returns Ok with profile data in the body" in {

      val profileData = HistoricProfileData(testEori, testEori, Some("UkimsNumber"), None, None)

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok().withBody(Json.toJson(profileData).toString))
      )

      connectorWithRouterConfig.getHistoricProfileData(testEori).futureValue mustBe Some(profileData)
    }

    "must return None when the error is forbidden" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(forbidden())
      )

      connectorWithRouterConfig.getHistoricProfileData(testEori).futureValue mustBe None
    }

    "must return a failed future when the error isn't forbidden" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .willReturn(serverError())
      )

      connectorWithRouterConfig.getHistoricProfileData(testEori).failed.futureValue
    }
  }
}
