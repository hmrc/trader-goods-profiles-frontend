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
import models.ott.OttResponseStore
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.{BAD_REQUEST, OK, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
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

    val ottResponse = "{\"data\": {\"id\": \"1234\", \"type\": \"some_type\"}, \"included\": []}"
    val nonParsableOttResponse = "{\"well_formed\": {\"but\": \"incorrect\", \"json\": \"right\"}, \"here\": []}"

    "getGoodsNomenclatures should return an OttResponseStore object when OTT response can be parsed" in {
      doReturn(Future.successful(HttpResponse(OK, ottResponse)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val expectedStore = new OttResponseStore(Json.parse(ottResponse))

      whenReady(
        ottConnector.getGoodsNomenclatures(testComcode),
        PatienceConfiguration.Timeout(Span(5, Seconds))
      ) { store =>
        store.getRoot shouldBe expectedStore.getRoot
      }
    }

    "getGoodsNomenclatures should error when the OTT response cannot be parsed " in {

      doReturn(Future.successful(HttpResponse(OK, nonParsableOttResponse)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val exception = intercept[Exception] {
        whenReady(
          ottConnector.getGoodsNomenclatures(testComcode),
          PatienceConfiguration.Timeout(Span(5, Seconds))
        ) { store =>
          store
        }
      }

      assert(exception.getMessage.contains("Error communicating with OTT: JsResultException"))
    }

    "getGoodsNomenclatures should error when the OTT resadfgsdfhgponse cannot be parsed " in {
      doReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ottResponse)))
        .when(ottConnector).requestDataFromOtt(testComcode)(mockHeaderCarrier)

      val exception = intercept[Exception] {
        whenReady(
          ottConnector.getGoodsNomenclatures(testComcode),
          PatienceConfiguration.Timeout(Span(5, Seconds))
        ) { store =>
          store
        }
      }

      assert(exception.getMessage.contains("Error communicating with OTT: OTT responded with status 500"))
    }

  }
}