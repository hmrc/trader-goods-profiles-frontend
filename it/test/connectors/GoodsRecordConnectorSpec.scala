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
import models.router.{CreateOrUpdateRecordResponse, CreateRecordRequest, UpdateRecordRequest}
import models.{CreateGoodsRecordResponse, GoodsRecord}
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

  private val xClientIdName: String = "X-Client-ID"
  private val xClientId: String     = "tgp-frontend"
  private def goodsRecordUrl        = s"/trader-goods-profiles-router/traders/$testEori/records"
  private val updateGoodsRecordUrl  = s"/trader-goods-profiles-router/records"
  private val instant               = Instant.now

  private val goodsRecord = GoodsRecord(
    testEori,
    "1",
    "2",
    "3",
    "4",
    instant,
    None,
    "recordId"
  )

  ".submitGoodsRecord" - {

    val createRecordRequest = CreateRecordRequest(
      testEori,
      testEori,
      "1",
      "2",
      "3",
      "4",
      instant,
      None
    )

    "must submit a goods record" in {

      val createGoodsRecordResponse = CreateGoodsRecordResponse("recordId")

      wireMockServer.stubFor(
        post(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(createRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(Json.toJson(createGoodsRecordResponse).toString))
      )

      connector.submitGoodsRecord(goodsRecord).futureValue mustBe createGoodsRecordResponse
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(createRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.submitGoodsRecord(goodsRecord).failed.futureValue
    }
  }

  ".updateGoodsRecord" - {

    val updateRecordRequest = UpdateRecordRequest(
      testEori,
      "recordId",
      testEori,
      Some("1"),
      Some("2"),
      Some("3"),
      Some("4"),
      None,
      None,
      None,
      None,
      Some(instant),
      None
    )

    "must update a goods record" in {

      val createOrUpdateRecordResponse = CreateOrUpdateRecordResponse(
        "recordId",
        "eori",
        "eori",
        "traderRef",
        "comcode",
        "accreditationStatus",
        "goodsDescription",
        "countryOfOrigin",
        1,
        None,
        None,
        None,
        instant,
        Some(instant),
        1,
        true,
        true,
        None,
        "declarable",
        None,
        None,
        None,
        instant,
        instant
      )

      wireMockServer.stubFor(
        put(urlEqualTo(updateGoodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(Json.toJson(createOrUpdateRecordResponse).toString))
      )

      connector.updateGoodsRecord(goodsRecord).futureValue mustBe createOrUpdateRecordResponse
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        put(urlEqualTo(updateGoodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateGoodsRecord(goodsRecord).failed.futureValue
    }
  }

}
