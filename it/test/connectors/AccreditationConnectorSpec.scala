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

import base.TestConstants.{testEori, testRecordId, withDrawReason}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.AdviceRequest
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class AccreditationConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[AccreditationConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".submitAdviceRequest" - {

    val adviceRequest =
      AdviceRequest("eori", "Firstname Lastname", "actorId", testRecordId, "test@test.com")

    "must submit a advice request" in {

      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/${adviceRequest.eori}/records/${adviceRequest.recordId}/advice"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .withRequestBody(equalTo(Json.toJson(adviceRequest).toString))
          .willReturn(ok())
      )

      connector.submitRequestAccreditation(adviceRequest).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/${adviceRequest.eori}/records/${adviceRequest.recordId}/advice"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .withRequestBody(equalTo(Json.toJson(adviceRequest).toString))
          .willReturn(serverError())
      )

      connector.submitRequestAccreditation(adviceRequest).failed.futureValue
    }
  }

  ".submitWithdrawAdviceRequest" - {

    "must submit a withdraw advice request" in {

      wireMockServer.stubFor(
        put(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$testEori/records/$testRecordId/advice"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .withRequestBody(equalToJson(s"""{"withdrawReason": "$withDrawReason"}"""))
          .willReturn(ok())
      )

      connector.withDrawRequestAccreditation(testEori, testRecordId, Some(withDrawReason)).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        put(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$testEori/records/$testRecordId/advice"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .withRequestBody(equalToJson(s"""{"withdrawReason": "$withDrawReason"}"""))
          .willReturn(serverError())
      )

      connector.withDrawRequestAccreditation(testEori, testRecordId, Some(withDrawReason)).failed.futureValue
    }
  }
}
