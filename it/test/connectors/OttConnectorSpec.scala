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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.Commodity
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream4xxResponse
import uk.gov.hmrc.http.test.WireMockSupport


class OttConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.online-trade-tariff-api.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[OttConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".getCommodityCode" - {

    "must return correct commodity object" in {

      val commodity = Commodity("123456", "Commodity description")

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(
            ok().withBody(
              "{\n  \"data\": {\n    \"attributes\": {\n      \"description\": \"Commodity description\",\n      \"goods_nomenclature_item_id\":\"123456\"\n    }\n  }\n}"
            )
          )
      )

      connector.getCommodityCode("123456").futureValue mustBe commodity
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(serverError())
      )

      connector.getCommodityCode("123456").failed.futureValue
    }

    "must return a not found future when the server returns a not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/commodities/123456"))
          .willReturn(notFound())
      )

      val connectorFailure = connector.getCommodityCode("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream4xxResponse] mustBe true
    }
  }

  ".getCategorisationInfo" - {

    "must return correct OttResponse object" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/goods-nomenclatures/123456"))
          .willReturn(
            ok().withBody(
              """{
                |  "data": {
                |    "id": "54267",
                |    "type": "goods_nomenclature",
                |    "attributes": {
                |      "goods_nomenclature_item_id": "9306210000"
                |    },
                |    "relationships": {
                |      "applicable_category_assessments": {
                |        "data": [
                |          {
                |            "id": "238dbab8cc5026c67757c7e05751f312",
                |            "type": "category_assessment"
                |          }
                |        ]
                |      }
                |    }
                |  },
                |  "included": [
                |    {
                |      "id": "238dbab8cc5026c67757c7e05751f312",
                |      "type": "category_assessment",
                |      "relationships": {
                |        "exemptions": {
                |          "data": [
                |            {
                |              "id": "8392",
                |              "type": "additional_code"
                |            }
                |          ]
                |        },
                |        "theme": {
                |          "data": {
                |            "id": "1.1",
                |            "type": "theme"
                |          }
                |        },
                |        "geographical_area": {
                |          "data": {
                |            "id": "IQ",
                |            "type": "geographical_area"
                |          }
                |        },
                |        "excluded_geographical_areas": {
                |          "data": [
                |
                |          ]
                |        },
                |        "measure_type": {
                |          "data": {
                |            "id": "465",
                |            "type": "measure_type"
                |          }
                |        },
                |        "regulation": {
                |          "data": {
                |            "id": "R0312100",
                |            "type": "legal_act"
                |          }
                |        },
                |        "measures": {
                |          "data": [
                |            {
                |              "id": "2524368",
                |              "type": "measure"
                |            }
                |          ]
                |        }
                |      }
                |    }
                |  ]
                |}""".stripMargin
            )
          )
      )

      val connectorResponse = connector.getCategorisationInfo("123456").futureValue
      connectorResponse.categoryAssessments.size mustEqual 1
      connectorResponse.categoryAssessments(0).id mustEqual "238dbab8cc5026c67757c7e05751f312"
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/goods-nomenclatures/123456"))
          .willReturn(serverError())
      )

      connector.getCategorisationInfo("123456").failed.futureValue
    }

    "must return a not found future when the server returns a not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/goods-nomenclatures/123456"))
          .willReturn(notFound())
      )

      val connectorFailure = connector.getCategorisationInfo("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream4xxResponse] mustEqual true
    }
  }
}
