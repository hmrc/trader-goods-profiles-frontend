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
import models.{CategoryRecord, GoodsRecord}
import models.router.requests.{CreateRecordRequest, UpdateRecordRequest}
import models.router.responses.{CreateGoodsRecordResponse, GetGoodsRecordResponse}
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

  private val testRecordId           = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private lazy val getRecordResponse = Json
    .parse(s"""
         |  {
         |    "eori": "$testEori",
         |    "actorId": "$testEori",
         |    "recordId": "$testRecordId",
         |    "traderRef": "BAN001001",
         |    "comcode": "10410100",
         |    "accreditationStatus": "Not requested",
         |    "goodsDescription": "Organic bananas",
         |    "countryOfOrigin": "EC",
         |    "category": 3,
         |    "assessments": [
         |      {
         |        "assessmentId": "abc123",
         |        "primaryCategory": "1",
         |        "condition": {
         |          "type": "abc123",
         |          "conditionId": "Y923",
         |          "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
         |          "conditionTraderText": "Excluded product"
         |        }
         |      }
         |    ],
         |    "supplementaryUnit": 500,
         |    "measurementUnit": "square meters(m^2)",
         |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
         |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z",
         |    "version": 1,
         |    "active": true,
         |    "toReview": false,
         |    "reviewReason": null,
         |    "declarable": "IMMI declarable",
         |    "ukimsNumber": "XIUKIM47699357400020231115081800",
         |    "nirmsNumber": "RMS-GB-123456",
         |    "niphlNumber": "6 S12345",
         |    "locked": false,
         |    "createdDateTime": "2024-11-18T23:20:19Z",
         |    "updatedDateTime": "2024-11-18T23:20:19Z"
         |  }
         |""".stripMargin)

  private val getUpdateGoodsRecordUrl    = s"/trader-goods-profiles-router/traders/$testEori/records/$testRecordId"

  private val instant = Instant.now

  ".submitGoodsRecord" - {

    val goodsRecord = GoodsRecord(
      testEori,
      "1",
      "2",
      "3",
      "4",
      instant,
      None
    )

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

      val createGoodsRecordResponse = CreateGoodsRecordResponse(testRecordId)

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

    val goodsRecord = CategoryRecord(
      eori = testEori,
      recordId = testRecordId,
      category = 1,
      measurementUnit = Some("1")
    )

    val updateRecordRequest = UpdateRecordRequest(
      testEori,
      testRecordId,
      testEori,
      Some(1),
      None,
      Some("1")
    )

    "must update a goods record" in {

      wireMockServer.stubFor(
        put(urlEqualTo(getUpdateGoodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok())
      )

      connector.updateGoodsRecord(testEori, testRecordId, goodsRecord).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        put(urlEqualTo(getUpdateGoodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateGoodsRecord(testEori, testRecordId, goodsRecord).failed.futureValue
    }
  }

  ".getRecord" - {

    "must get a goods record" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getUpdateGoodsRecordUrl))
          .willReturn(ok().withBody(getRecordResponse.toString))
      )

      connector.getRecord(testEori, testRecordId).futureValue mustBe GetGoodsRecordResponse(
        testRecordId,
        "10410100",
        "EC"
      )
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getUpdateGoodsRecordUrl))
          .willReturn(serverError())
      )

      connector.getRecord(testEori, testRecordId).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getUpdateGoodsRecordUrl))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getRecord(testEori, testRecordId).failed.futureValue
    }
  }
}
