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

import base.SpecBase
import config.FrontendAppConfig
import models.router.requests.SetUpProfileRequest
import models.{Eori, NiphlNumber, NirmsNumber, ServiceDetails, TraderGoodsProfile, UkimsNumber}
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class RouterConnectorSpec extends SpecBase {
  private val appConfig      = mock[FrontendAppConfig]
  private val httpClient     = mock[HttpClientV2]
  private val requestBuilder = mock[RequestBuilder]

  private val routerConnector = new RouterConnector(appConfig, httpClient)

  private val userEnteredData = TraderGoodsProfile(
    Some(UkimsNumber("ukims")),
    Some(true),
    Some(NirmsNumber("nirms")),
    Some(true),
    Some(NiphlNumber("niphl"))
  )

  private val expectedRouterRequest = SetUpProfileRequest(
    "eori",
    Some("ukims"),
    Some("nirms"),
    Some("niphl")
  )

  "Router Connector" - {
    "setUpProfile" - {
      "must return response when successful" in {


        when(appConfig.tgpRouter).thenReturn(ServiceDetails("http", "host", 1))
        when(httpClient.put(any)(any)).thenReturn(requestBuilder)

        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(successful(HttpResponse(NO_CONTENT, "")))

        val result = await(
          routerConnector.setUpProfile(
            Eori("eori"),
            userEnteredData
          )
        )

        result mustBe Right()

        withClue("should have submitted the expected request") {
          verify(requestBuilder.withBody(eqTo(expectedRouterRequest))(any, any, any))
        }

      }

      "must return an Upstream Error if router returns one" in {

        val userEnteredData = TraderGoodsProfile(
          Some(UkimsNumber("ukims")),
          Some(true),
          Some(NirmsNumber("nirms")),
          Some(true),
          Some(NiphlNumber("niphl"))
        )

        val expectedRouterRequest = SetUpProfileRequest(
          "eori",
          Some("ukims"),
          Some("nirms"),
          Some("niphl")
        )

        when(appConfig.tgpRouter).thenReturn(ServiceDetails("http", "host", 1))
        when(httpClient.put(any)(any)).thenReturn(requestBuilder)

        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(successful(HttpResponse(BAD_REQUEST, "Bad json")))

        val result = await(
          routerConnector.setUpProfile(
            Eori("eori"),
            userEnteredData
          )
        )

        result match {
          case Left(error) => {
            error.getMessage() must include("Bad json")
            error.statusCode mustBe BAD_REQUEST
          }
          case Right(_) => fail()
        }

      }

      //TODO finish test
      "must return an Upstream Error if exception thrown by http client" in {

        when(appConfig.tgpRouter).thenReturn(ServiceDetails("http", "host", 1))
        when(httpClient.put(any)(any)).thenReturn(requestBuilder)

        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(failed(new RuntimeException("request failed")))

        val result = await(
          routerConnector.setUpProfile(
            Eori("eori"),
            userEnteredData
          )
        )

        result match {
          case Left(error) => {
            error.getMessage() must include("request failed")
            error.statusCode mustBe 0
          }
          case Right(_) => fail()
        }

      }

    }

  }

}
