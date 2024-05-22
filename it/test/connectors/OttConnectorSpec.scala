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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.Commodity
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class OttConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.online-trade-tariff-api.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[OttConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".getCommodityCode" - {

    "must return correct commodity object" in {

      val commodity = Commodity("123456", "Commodity description")

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(
            ok().withBody(
              "{\n  \"data\": {\n    \"attributes\": {\n      \"description\": \"Commodity description\",\n      \"goods_nomenclature_item_id\":\"123456\"\n    }\n  }\n}"
            )
          )
      )

      connector.getCommodityCode("123456").futureValue mustBe commodity
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(serverError())
      )

      connector.getCommodityCode("123456").failed.futureValue
    }

    "must return a not found future when the server returns an not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(notFound())
      )

      connector.getCommodityCode("123456").failed.futureValue
    }

    "must fail with JsResult.Exception when the JSON response is invalid" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(ok().withBody("{ invalid json }"))
      )

      val result = connector.getCommodityCode("123456").failed.futureValue

      result mustBe a[JsResult.Exception]
    }

  }
}
