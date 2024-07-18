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
import models.{CategoryRecord, Commodity, GoodsRecord, UpdateGoodsRecord}
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
  private val testRecordId          = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val goodsRecordUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId"

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

  private val instant = Instant.now

  ".submitGoodsRecord" - {

    val routerGoodsRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records"

    val goodsRecord = GoodsRecord(
      testEori,
      "1",
      Commodity("2", List("desc"), instant, None),
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
        post(urlEqualTo(routerGoodsRecordsUrl))
          .withRequestBody(equalTo(Json.toJson(createRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(Json.toJson(createGoodsRecordResponse).toString))
      )

      connector.submitGoodsRecord(goodsRecord).futureValue mustBe createGoodsRecordResponse
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(routerGoodsRecordsUrl))
          .withRequestBody(equalTo(Json.toJson(createRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.submitGoodsRecord(goodsRecord).failed.futureValue
    }
  }

  ".removeGoodsRecord" - {

    val removeRecordUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId"

    "must remove a goods record" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(noContent())
      )

      connector.removeGoodsRecord(testEori, testRecordId).futureValue mustBe true
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.removeGoodsRecord(testEori, testRecordId).failed.futureValue
    }

    "must return a failed future when the server returns not found" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector.removeGoodsRecord(testEori, testRecordId).futureValue mustBe false
    }
  }

  ".updateCategoryForGoodsRecord" - {

    val goodsRecord = CategoryRecord(
      eori = testEori,
      recordId = testRecordId,
      category = 1,
      categoryAssessmentsWithExemptions = 3,
      measurementUnit = Some("1"),
      supplementaryUnit = Some("123")
    )

    val updateRecordRequest = UpdateRecordRequest(
      testEori,
      testRecordId,
      testEori,
      category = Some(1),
      supplementaryUnit = Some(123),
      measurementUnit = Some("1")
    )

    "must update a goods record" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok())
      )

      connector.updateCategoryForGoodsRecord(testEori, testRecordId, goodsRecord).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateCategoryForGoodsRecord(testEori, testRecordId, goodsRecord).failed.futureValue
    }
  }

  ".updateGoodsRecord" - {

    val goodsRecord = UpdateGoodsRecord(
      eori = testEori,
      recordId = testRecordId,
      countryOfOrigin = Some("CN")
    )

    val updateRecordRequest = UpdateRecordRequest(
      testEori,
      testRecordId,
      testEori,
      Some("CN"),
      None,
      None,
      None
    )

    "must update a goods record" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok())
      )

      connector.updateGoodsRecord(goodsRecord).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateGoodsRecord(goodsRecord).failed.futureValue
    }
  }

  ".getRecord" - {

    val dataStoreGoodsRecordUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId"

    "must get a goods record" in {

      wireMockServer.stubFor(
        get(urlEqualTo(dataStoreGoodsRecordUrl))
          .willReturn(ok().withBody(getRecordResponse.toString))
      )

      connector.getRecord(testEori, testRecordId).futureValue mustBe getRecordResponse
        .validate[GetGoodsRecordResponse]
        .get
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(dataStoreGoodsRecordUrl))
          .willReturn(serverError())
      )

      connector.getRecord(testEori, testRecordId).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(dataStoreGoodsRecordUrl))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getRecord(testEori, testRecordId).failed.futureValue
    }
  }

  ".getRecords" - {

    val pagedGoodsRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records?page=1&size=3"

    "must get a page of goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(getRecordsResponse.toString))
      )

      connector.getRecords(testEori, 1, 3).futureValue mustBe getRecordsResponse.validate[GetRecordsResponse].get
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.getRecords(testEori, 1, 3).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getRecords(testEori, 1, 3).failed.futureValue
    }
  }

  ".getRecordsCount" - {

    val getRecordsCountUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/count"

    "must get the number of records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getRecordsCountUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("3"))
      )

      connector.getRecordsCount(testEori).futureValue mustBe 3
    }

    "must return 0 if there are no records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getRecordsCountUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("0"))
      )

      connector.getRecordsCount(testEori).futureValue mustBe 0
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getRecordsCountUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.getRecordsCount(testEori).failed.futureValue
    }

  }

  ".storeAllRecords" - {

    val storeAllRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/store"

    "must store all goods records" in {

      wireMockServer.stubFor(
        head(urlEqualTo(storeAllRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(noContent())
      )

      connector
        .storeAllRecords(testEori)
        .futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer
        .stubFor(
          head(urlEqualTo(storeAllRecordsUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(serverError())
        )
      connector.storeAllRecords(testEori).failed.futureValue
    }
  }

  ".storeLatestRecords" - {

    val storeLatestRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records/storeLatest"

    "must store latest goods records" in {

      wireMockServer.stubFor(
        head(urlEqualTo(storeLatestRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(noContent())
      )

      connector
        .storeLatestRecords(testEori)
        .futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer
        .stubFor(
          head(urlEqualTo(storeLatestRecordsUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(serverError())
        )
      connector.storeLatestRecords(testEori).failed.futureValue
    }

    "must return a failed future when the server can't find the latest record in the data store" in {

      wireMockServer
        .stubFor(
          head(urlEqualTo(storeLatestRecordsUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(notFound())
        )
      connector.storeLatestRecords(testEori).failed.futureValue
    }
  }

  ".doRecordsExist" - {

    val checkRecordsUrl = s"/trader-goods-profiles-data-store/traders/$testEori/checkRecords"

    "must return true if goods records for that eori have already been stored once" in {

      wireMockServer.stubFor(
        head(urlEqualTo(checkRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(noContent())
      )

      connector
        .doRecordsExist(testEori)
        .futureValue mustEqual true
    }

    "must return false if goods records for that eori have not been stored" in {

      wireMockServer.stubFor(
        head(urlEqualTo(checkRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector
        .doRecordsExist(testEori)
        .futureValue mustEqual false
    }

    "must return failed future if server error" in {

      wireMockServer
        .stubFor(
          head(urlEqualTo(checkRecordsUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(serverError())
        )
      connector.doRecordsExist(testEori).failed.futureValue
    }
  }

  ".filterRecordsByField" - {

    val filterRecordsUrl =
      s"/trader-goods-profiles-data-store/traders/$testEori/records/filter?searchTerm=TOM001001&field=traderRef"

    "must get a page of goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(filterRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(getRecordsResponse.toString))
      )

      connector.filterRecordsByField(testEori, "TOM001001", "traderRef").futureValue mustBe getRecordsResponse
        .validate[GetRecordsResponse]
        .get
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(filterRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.filterRecordsByField(testEori, "TOM001001", "traderRef").failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(filterRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.filterRecordsByField(testEori, "TOM001001", "traderRef").failed.futureValue
    }
  }

  ".getSearch" - {

    val searchString               = "banana"
    val exactMatch                 = false
    val pagedGoodsRecordsSearchUrl =
      s"/trader-goods-profiles-data-store/traders/$testEori/records/filter?searchTerm=$searchString&exactMatch=$exactMatch&page=1&size=3"

    "must get a page of goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsSearchUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(getRecordsResponse.toString))
      )

      connector.searchRecords(testEori, searchString, exactMatch = false, 1, 3).futureValue mustBe getRecordsResponse
        .validate[GetRecordsResponse]
        .get
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsSearchUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.searchRecords(testEori, searchString, exactMatch = false, 1, 3).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsSearchUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.searchRecords(testEori, searchString, exactMatch = false, 1, 3).failed.futureValue
    }
  }
}
