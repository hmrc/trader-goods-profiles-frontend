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
import models.ott.CategorisationInfo
import models.router.requests.{CreateRecordRequest, PatchRecordRequest, PutRecordRequest}
import models.router.responses.{GetGoodsRecordResponse, GetRecordsResponse}
import models.{Category1Scenario, CategoryRecord, Commodity, GoodsRecord, RecordsSummary, SupplementaryRequest, UpdateGoodsRecord}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.ACCEPTED
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
    with GetRecordsResponseUtil
    with OptionValues {

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

  private val goodsRecordUrl    = s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId"
  private val recordsSummaryUrl = s"/trader-goods-profiles-data-store/traders/$testEori/records-summary"

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

  val goodsRecord: GetGoodsRecordResponse = getRecordResponse.as[GetGoodsRecordResponse]

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
      None,
      None
    )

    "must submit a goods record" in {

      wireMockServer.stubFor(
        post(urlEqualTo(routerGoodsRecordsUrl))
          .withRequestBody(equalTo(Json.toJson(createRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(testRecordId))
      )

      connector.submitGoodsRecord(goodsRecord).futureValue mustBe testRecordId
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

    "must return true when goods record is removed" in {

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

    "must return false when the server returns not found" in {

      wireMockServer.stubFor(
        delete(urlEqualTo(removeRecordUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector.removeGoodsRecord(testEori, testRecordId).futureValue mustBe false
    }
  }

  ".updateCategoryAndComcodeForGoodsRecord" - {

    val categoryRecord = CategoryRecord(
      eori = testEori,
      recordId = testRecordId,
      category = Category1Scenario,
      assessmentsAnswered = 3,
      finalComCode = "1234567890",
      measurementUnit = None,
      supplementaryUnit = None,
      initialCategoryInfo = CategorisationInfo("1234567890", "BV", None, Seq.empty, Seq.empty, None, 0),
      wasSupplementaryUnitAsked = false
    )

    val updateRecordRequest = PatchRecordRequest(
      testEori,
      testRecordId,
      testEori,
      category = Some(1),
      supplementaryUnit = None,
      measurementUnit = None,
      comcode = Some("1234567890")
    )

    "must update a goods record" - {
      "with a category" in {

        wireMockServer.stubFor(
          patch(urlEqualTo(goodsRecordUrl))
            .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(ok())
        )

        connector.updateCategoryAndComcodeForGoodsRecord(testEori, testRecordId, categoryRecord, goodsRecord).futureValue
      }

      "with a category and supplementary unit" in {

        val categoryRecordWithSupp = categoryRecord.copy(
          measurementUnit = Some("weight"),
          supplementaryUnit = Some("123")
        )

        val updateRecordRequestWithSupp = updateRecordRequest.copy(
          measurementUnit = Some("weight"),
          supplementaryUnit = Some(123)
        )

        wireMockServer.stubFor(
          patch(urlEqualTo(goodsRecordUrl))
            .withRequestBody(equalTo(Json.toJson(updateRecordRequestWithSupp).toString))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(ok())
        )

        connector.updateCategoryAndComcodeForGoodsRecord(testEori, testRecordId, categoryRecordWithSupp, goodsRecord).futureValue
      }

      "with a category and longer commodity code" in {

        val categoryRecordWithLongerComCode = categoryRecord.copy(
          finalComCode = "9988776655"
        )

        val updateRecordWithLongerComCode = updateRecordRequest.copy(
          comcode = Some("9988776655")
        )

        wireMockServer.stubFor(
          patch(urlEqualTo(goodsRecordUrl))
            .withRequestBody(equalTo(Json.toJson(updateRecordWithLongerComCode).toString))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(ok())
        )

        connector
          .updateCategoryAndComcodeForGoodsRecord(testEori, testRecordId, categoryRecordWithLongerComCode, goodsRecord)
          .futureValue
      }

    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateCategoryAndComcodeForGoodsRecord(testEori, testRecordId, categoryRecord, goodsRecord).failed.futureValue
    }
  }

  ".updateSupplementaryUnitForGoodsRecord" - {

    val supplementaryRequest = SupplementaryRequest(
      eori = testEori,
      recordId = testRecordId,
      hasSupplementaryUnit = Some(true),
      supplementaryUnit = Some("123"),
      measurementUnit = Some("1")
    )

    val updateRecordRequest = PatchRecordRequest(
      testEori,
      testRecordId,
      testEori,
      category = None,
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

      connector.updateSupplementaryUnitForGoodsRecord(testEori, testRecordId, supplementaryRequest, goodsRecord).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.updateSupplementaryUnitForGoodsRecord(testEori, testRecordId, supplementaryRequest, goodsRecord).failed.futureValue
    }
  }

  ".patchGoodsRecord" - {

    val goodsRecord = UpdateGoodsRecord(
      eori = testEori,
      recordId = testRecordId,
      countryOfOrigin = Some("CN")
    )

    val updateRecordRequest = PatchRecordRequest(
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

  ".putGoodsRecord" - {

    val updateRecordRequest = PutRecordRequest(
      testEori,
      "BAN001001",
      "10410100",
      "Organic bananas",
      "EC",
      Some(3),
      None,
      Some(500),
      Some("square meters(m^2)"),
      Instant.parse("2024-10-12T16:12:34Z"),
      Some(Instant.parse("2024-10-12T16:12:34Z"))
    )

    "must update a goods record" in {

      wireMockServer.stubFor(
        put(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok())
      )

      connector.putGoodsRecord(updateRecordRequest, testRecordId).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        patch(urlEqualTo(goodsRecordUrl))
          .withRequestBody(equalTo(Json.toJson(updateRecordRequest).toString))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.putGoodsRecord(updateRecordRequest, testRecordId).failed.futureValue
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

      connector.getRecords(testEori, 1, 3).futureValue.value mustEqual getRecordsResponse.as[GetRecordsResponse]
    }

    "must return done when the status is ACCEPTED" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(status(ACCEPTED))
      )

      connector.getRecords(testEori, 1, 3).futureValue mustBe None
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

  ".filterRecordsByField" - {

    val filterRecordsUrl =
      s"/trader-goods-profiles-data-store/traders/$testEori/records/filter?searchTerm=TOM001001&field=traderRef"

    "must get a page of goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(filterRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(getRecordsResponse.toString))
      )

      connector.filterRecordsByField(testEori, "TOM001001", "traderRef").futureValue.value mustEqual getRecordsResponse
        .as[GetRecordsResponse]
    }

    "must return done when the status is ACCEPTED" in {

      wireMockServer.stubFor(
        get(urlEqualTo(filterRecordsUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(status(ACCEPTED))
      )

      connector.filterRecordsByField(testEori, "TOM001001", "traderRef").futureValue mustBe None
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

      connector
        .searchRecords(testEori, searchString, exactMatch = false, 1, 3)
        .futureValue
        .value mustBe getRecordsResponse.as[GetRecordsResponse]
    }

    "must return done when the status is ACCEPTED" in {

      wireMockServer.stubFor(
        get(urlEqualTo(pagedGoodsRecordsSearchUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(status(ACCEPTED))
      )

      connector.searchRecords(testEori, searchString, exactMatch = false, 1, 3).futureValue mustBe None
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

  ".getRecordsSummary" - {

    val recordsSummary = RecordsSummary(testEori, None, instant)

    "must get a records summary" in {

      wireMockServer.stubFor(
        get(urlEqualTo(recordsSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(Json.toJson(recordsSummary).toString))
      )

      connector.getRecordsSummary(testEori).futureValue.eori mustBe recordsSummary.eori
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(recordsSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.getRecordsSummary(testEori).failed.futureValue
    }
  }
}
