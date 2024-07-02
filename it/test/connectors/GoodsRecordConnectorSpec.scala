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
import models.router.requests.{CreateRecordRequest, UpdateRecordRequest}
import models.router.responses.{CreateGoodsRecordResponse, GetGoodsRecordResponse, GetRecordsResponse}
import models.{CategoryRecord, Commodity, GoodsRecord, GoodsRecordsPagination}
import org.apache.pekko.Done
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import utils.GetRecordsResponseUtil

import java.time.Instant

class GoodsRecordConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GetRecordsResponseUtil {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[GoodsRecordConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val xClientIdName: String = "X-Client-ID"
  private val xClientId: String     = "tgp-frontend"
  private def goodsRecordUrl        = s"/trader-goods-profiles-router/traders/$testEori/records"
  private def getGoodsRecordsUrl    =
    s"/trader-goods-profiles-data-store/traders/$testEori/records"

  private val testRecordId           = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private lazy val getRecordResponse = Json
    .parse(s"""
         |  {
         |    "eori": "$testEori",
         |    "actorId": "$testEori",
         |    "recordId": "$testRecordId",
         |    "traderRef": "BAN001001",
         |    "comcode": "10410100",
         |    "adviceStatus": "Not requested",
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

  private lazy val getRecordsResponse = Json
    .parse(s"""
              |{
              |"goodsItemRecords": [
              |  {
              |    "eori": "$testEori",
              |    "actorId": "$testEori",
              |    "recordId": "1",
              |    "traderRef": "BAN0010011",
              |    "comcode": "10410100",
              |    "adviceStatus": "Not requested",
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
              |    "createdDateTime": "2022-11-18T23:20:19Z",
              |    "updatedDateTime": "2022-11-18T23:20:19Z"
              |  },
              |    {
              |    "eori": "$testEori",
              |    "actorId": "$testEori",
              |    "recordId": "2",
              |    "traderRef": "BAN0010012",
              |    "comcode": "10410100",
              |    "adviceStatus": "Not requested",
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
              |    "createdDateTime": "2023-11-18T23:20:19Z",
              |    "updatedDateTime": "2023-11-18T23:20:19Z"
              |  },
              |    {
              |    "eori": "$testEori",
              |    "actorId": "$testEori",
              |    "recordId": "3",
              |    "traderRef": "BAN0010013",
              |    "comcode": "10410100",
              |    "adviceStatus": "Not requested",
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
              |  ],
              |  "pagination": {
              |  "totalRecords": 10,
              |  "currentPage": 1,
              |  "totalPages": 4
              |  }
              |  }
              |""".stripMargin)

  private val getUpdateGoodsRecordUrl = s"/trader-goods-profiles-router/traders/$testEori/records/$testRecordId"

  private val instant = Instant.now

  ".submitGoodsRecord" - {

    val goodsRecord = GoodsRecord(
      testEori,
      "1",
      Commodity("2", "desc", instant, None),
      "3",
      "4"
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

  ".removeGoodsRecord" - {

    val removeGoodsRecordUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId"

    "must remove a goods record" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeGoodsRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(noContent())
      )

      connector.removeGoodsRecord(testEori, testRecordId).futureValue mustBe Done
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeGoodsRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.removeGoodsRecord(testEori, testRecordId).failed.futureValue
    }

    "must return a failed future when the server returns not found" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeGoodsRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector.removeGoodsRecord(testEori, testRecordId).failed.futureValue
    }
  }

  ".updateGoodsRecord" - {

    val goodsRecord = CategoryRecord(
      eori = testEori,
      recordId = testRecordId,
      category = 1,
      categoryAssessmentsWithExemptions = 3,
      measurementUnit = Some("1"),
      supplementaryUnit = Some("123.123")
    )

    val updateRecordRequest = UpdateRecordRequest(
      testEori,
      testRecordId,
      testEori,
      Some(1),
      Some(123.123),
      Some("1")
    )

    "must update a goods record" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(getUpdateGoodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok())
      )

      connector.updateGoodsRecord(testEori, testRecordId, goodsRecord).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(getUpdateGoodsRecordUrl))
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
        "EC",
        "BAN001001",
        "Organic bananas",
        "IMMI declarable",
        Instant.parse("2024-11-18T23:20:19Z"),
        Instant.parse("2024-11-18T23:20:19Z")
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

  ".getRecords" - {

    val goodsRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records?page=1&size=3"

    "must get a page of goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(goodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(getRecordsResponse.toString))
      )

      connector.getRecords(testEori, Some(1), Some(3)).futureValue mustBe GetRecordsResponse(
        Seq(
          GetGoodsRecordResponse(
            "1",
            "10410100",
            "EC",
            "BAN0010011",
            "Organic bananas",
            "IMMI declarable",
            Instant.parse("2022-11-18T23:20:19Z"),
            Instant.parse("2022-11-18T23:20:19Z")
          ),
          GetGoodsRecordResponse(
            "2",
            "10410100",
            "EC",
            "BAN0010012",
            "Organic bananas",
            "IMMI declarable",
            Instant.parse("2023-11-18T23:20:19Z"),
            Instant.parse("2023-11-18T23:20:19Z")
          ),
          GetGoodsRecordResponse(
            "3",
            "10410100",
            "EC",
            "BAN0010013",
            "Organic bananas",
            "IMMI declarable",
            Instant.parse("2024-11-18T23:20:19Z"),
            Instant.parse("2024-11-18T23:20:19Z")
          )
        ),
        GoodsRecordsPagination(10, 1, 4, None, None)
      )
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(goodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.getRecords(testEori, Some(1), Some(3)).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(goodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getRecords(testEori, Some(1), Some(3)).failed.futureValue
    }
  }

  ".getAllRecords" - {

    "must get goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getGoodsRecordsUrl))
          .willReturn(ok().withBody(getMultipleRecordResponseData.toString()))
      )

      connector
        .getAllRecords(testEori)
        .futureValue mustBe getMultipleRecordResponseData.as[GetRecordsResponse]
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer
        .stubFor(
          get(urlEqualTo(getGoodsRecordsUrl))
            .willReturn(serverError())
        )
      connector.getAllRecords(testEori).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getGoodsRecordsUrl))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getAllRecords(testEori).failed.futureValue
    }
  }

  ".doRecordsExist" - {

    "must check if goods records exist" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getGoodsRecordsUrl))
          .willReturn(ok().withBody(getMultipleRecordResponseData.toString()))
      )

      connector
        .doRecordsExist(testEori)
        .futureValue mustBe Some(getMultipleRecordResponseData.as[GetRecordsResponse])
    }

    "must check if goods not found for the eori" in {

      wireMockServer
        .stubFor(
          get(urlEqualTo(getGoodsRecordsUrl))
            .willReturn(serverError())
        )
      connector.doRecordsExist(testEori).failed.futureValue
    }

    "must check if goods records response is empty for the eori" in {

      wireMockServer
        .stubFor(
          get(urlEqualTo(getGoodsRecordsUrl))
            .willReturn(ok().withBody(getEmptyResponseData.toString()))
        )
      connector.doRecordsExist(testEori).futureValue mustBe Some(getEmptyResponseData.as[GetRecordsResponse])
    }

  }

}
