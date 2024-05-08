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
import models.{Eori, ServiceDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future.{failed, successful}

class RouterConnectorSpec extends SpecBase with BeforeAndAfterEach {
  private val appConfig      = mock[FrontendAppConfig]
  private val httpClient     = mock[HttpClientV2]
  private val requestBuilder = mock[RequestBuilder]

  private val routerConnector = new RouterConnector(appConfig, httpClient)

  private val requestData = SetUpProfileRequest(
    "eori",
    Some("ukims"),
    Some("nirms"),
    Some("niphl")
  )

  private val routerResponse =
    """
      |{
      | "actorId": "GB123456789012",
      | "ukimsNumber": "XI47699357400020231115081800",
      | "nirmsNumber": "RMS-GB-123456",
      | "niphlNumber": "S12345"
      |}
      |""".stripMargin

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, requestBuilder)

    when(appConfig.tgpRouter).thenReturn(ServiceDetails("http", "host", 1))
    when(httpClient.put(any)(any)).thenReturn(requestBuilder)

    when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)

  }

  "Router Connector" - {
    "setUpProfile" - {
      "must return response when successful" in {

        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(
            successful(
              HttpResponse(
                OK,
                routerResponse
              )
            )
          )

        val result =
          routerConnector.setUpProfile(
            Eori("eori"),
            requestData
          )

        await(result.value) mustBe Right()

        withClue("should have submitted the expected request") {
          verify(requestBuilder).withBody(eqTo(Json.toJson(requestData)))(any, any, any)
        }

      }

      "must return an error if router returns one" in {

        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(successful(HttpResponse(BAD_REQUEST, "Bad json")))

        val result =
          routerConnector.setUpProfile(
            Eori("eori"),
            requestData
          )

        await(result.value) match {
          case Left(error) =>
            error.errorMsg must include("Bad json")
            error.status mustBe Some(BAD_REQUEST)
          case Right(_)    => fail()
        }

      }

      "must return an Error if exception thrown by HTTP client" in {

        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(failed(new RuntimeException("request failed")))

        val result =
          routerConnector.setUpProfile(
            Eori("eori"),
            requestData
          )

        await(result.value) match {
          case Left(error) =>
            error.errorMsg must include("request failed")
            error.status mustBe None
          case Right(_)    => fail()
        }

      }

    }

  }

}
