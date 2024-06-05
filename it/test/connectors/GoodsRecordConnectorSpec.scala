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
import models.{CreateGoodsRecordRequest, CreateGoodsRecordResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.Instant

class GoodsRecordConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[GoodsRecordConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".submitGoodsRecord" - {

    val goodsRecord = CreateGoodsRecordRequest(
      testEori,
      testEori,
      "1",
      "2",
      "3",
      "4",
      1,
      None,
      None,
      None,
      Instant.now,
      None
    )

    "must submit a goods record" in {

      val goodsRecordResponse = CreateGoodsRecordResponse("recordId")

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/records"))
          .withRequestBody(equalTo(Json.toJson(goodsRecord).toString))
          .willReturn(ok().withBody(Json.toJson(goodsRecordResponse).toString))
      )

      connector.submitGoodsRecordUrl(goodsRecord, testEori).futureValue mustBe goodsRecordResponse
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/records"))
          .withRequestBody(equalTo(Json.toJson(goodsRecord).toString))
          .willReturn(serverError())
      )

      connector.submitGoodsRecordUrl(goodsRecord, testEori).failed.futureValue
    }
  }
}
