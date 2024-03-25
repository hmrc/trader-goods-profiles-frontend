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

import config.FrontendAppConfig
import models.ott.OttResponse
import org.scalatest._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.{ExecutionContext, Future}


class OttConnectorSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val testComcode = "testComcode"

  val ottConnector = spy(new OttConnector(mockHttpClient, mockAppConfig))

  "OttConnector" - {

    "getGoodsNomenclatures should return an OttResponse object when OTT response can be parsed" in {

      val ottResponseBody = "{\"data\": {}, \"included\": []}"

      doReturn(Future.successful(HttpResponse(OK, ottResponseBody)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val expectedOttResponseObject = OttResponse(data = JsObject.empty, included = List())

      whenReady(
        ottConnector.getGoodsNomenclatures(testComcode),
        PatienceConfiguration.Timeout(Span(5, Seconds))
      ) { response =>
        response shouldBe expectedOttResponseObject
      }
    }

    "getGoodsNomenclatures should return an exception when OTT response cannot be parsed" in {

      val ottResponseBody = "{\"abcd\": {}, \"abcdef\": []}"

      doReturn(Future.successful(HttpResponse(OK, ottResponseBody)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val exception = intercept[Exception] {
        whenReady(
          ottConnector.getGoodsNomenclatures(testComcode),
          PatienceConfiguration.Timeout(Span(5, Seconds))
        ) { response =>
          response
        }
      }

      assert(exception.getMessage.contains("Error communicating with OTT: Failed to parse OTT response:"))
    }

    "getGoodsNomenclatures should return an exception when OTT response is malformed" in {

      val ottResponseBody = "{\"abc...{d\": {}, \"abc{d}{ef\"+{: [{}]}"

      doReturn(Future.successful(HttpResponse(OK, ottResponseBody)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val exception = intercept[Exception] {
        whenReady(
          ottConnector.getGoodsNomenclatures(testComcode),
          PatienceConfiguration.Timeout(Span(5, Seconds))
        ) { response =>
          response
        }
      }

      assert(exception.getMessage.contains("Error communicating with OTT: Unexpected character"))
    }

    "getGoodsNomenclatures should return an exception when OTT responds with an error" in {

      doReturn(Future.successful(HttpResponse(BAD_REQUEST, "bad request")))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val exception = intercept[Exception] {
        whenReady(
          ottConnector.getGoodsNomenclatures(testComcode),
          PatienceConfiguration.Timeout(Span(5, Seconds))
        ) { response =>
          response
        }
      }

      assert(exception.getMessage.contains("Error communicating with OTT: Failure status from OTT. Code: 400  Body: bad request."))
    }

  }
}