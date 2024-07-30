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

import models.router.responses.GetGoodsRecordResponse
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json._
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.language.postfixOps

class BaseConnectorSpec extends AsyncWordSpec with Matchers with ScalaFutures with MockitoSugar {

  private lazy val getRecordResponse = Json
    .parse(s"""
         |  {
         |    "eori": "eori",
         |    "actorId": "eori",
         |    "recordId": "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
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

  val goodsItemRecords: GetGoodsRecordResponse = getRecordResponse.as[GetGoodsRecordResponse]

  trait TestBaseConnector extends BaseConnector

  "HttpResponseHelpers" should {
    "return a failed future with UpstreamErrorResponse when error is called" in {
      val response = mock[HttpResponse]
      val body     = "Error body"

      when(response.body).thenReturn(body)
      when(response.status).thenReturn(INTERNAL_SERVER_ERROR)

      val helper = new TestBaseConnector {}.HttpResponseHelpers(response)
      recoverToExceptionIf[UpstreamErrorResponse] {
        helper.error
      } map { ex =>
        ex.statusCode shouldBe INTERNAL_SERVER_ERROR
        ex.message    shouldBe body
      }
    }

    "return a successful future with deserialized object when as is called with valid json" in {

      val response = mock[HttpResponse]

      when(response.json).thenReturn(getRecordResponse)

      val helper = new TestBaseConnector {}.HttpResponseHelpers(response)
      helper.as[GetGoodsRecordResponse].map { result =>
        result shouldBe goodsItemRecords
      }
    }

  }

  "RequestBuilderHelpers" should {
    "return deserialized object when executeAndDeserialise is called with 200 response" in {

      val requestBuilder = mock[RequestBuilder]
      val response       = mock[HttpResponse]

      when(response.status).thenReturn(OK)
      when(response.json).thenReturn(getRecordResponse)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(response))

      val helper = new TestBaseConnector {}.RequestBuilderHelpers(requestBuilder)
      helper.executeAndDeserialise[GetGoodsRecordResponse].map { result =>
        result shouldBe goodsItemRecords
      }
    }

    "return failed future when executeAndDeserialise is called with non 2xx response" in {
      val requestBuilder = mock[RequestBuilder]
      val response       = mock[HttpResponse]

      when(response.status).thenReturn(INTERNAL_SERVER_ERROR)
      when(response.body).thenReturn("Error body")
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(response))

      val helper = new TestBaseConnector {}.RequestBuilderHelpers(requestBuilder)
      recoverToExceptionIf[UpstreamErrorResponse] {
        helper.executeAndDeserialise[String]
      } map { ex =>
        ex.statusCode shouldBe INTERNAL_SERVER_ERROR
        ex.message    shouldBe "Error body"
      }
    }

    "return successful future when executeAndContinue is called with 204 response" in {
      val requestBuilder = mock[RequestBuilder]
      val response       = mock[HttpResponse]

      when(response.status).thenReturn(NO_CONTENT)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(response))

      val helper = new TestBaseConnector {}.RequestBuilderHelpers(requestBuilder)
      helper.executeAndContinue.map { result =>
        result shouldBe ()
      }
    }

    "return failed future when executeAndContinue is called with non 2xx response" in {
      val requestBuilder = mock[RequestBuilder]
      val response       = mock[HttpResponse]

      when(response.status).thenReturn(INTERNAL_SERVER_ERROR)
      when(response.body).thenReturn("Error body")
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(response))

      val helper = new TestBaseConnector {}.RequestBuilderHelpers(requestBuilder)
      recoverToExceptionIf[UpstreamErrorResponse] {
        helper.executeAndContinue
      } map { ex =>
        ex.statusCode shouldBe INTERNAL_SERVER_ERROR
        ex.message    shouldBe "Error body"
      }
    }
  }

}
