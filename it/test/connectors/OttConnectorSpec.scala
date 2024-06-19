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
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.Instant

class OttConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.online-trade-tariff-api.url" -> wireMockUrl)
      .build()

  private lazy val connector = app.injector.instanceOf[OttConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".getCommodityCode" - {

    "must return correct commodity object" - {

      "when validity end date is undefined" in {
        val commodity = Commodity("123456", "Commodity description", Instant.parse("2012-01-01T00:00:00.000Z"), None)

        wireMockServer.stubFor(
          get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
            .willReturn(
              ok().withBody(
                "{\n  \"data\": {\n    \"attributes\": {\n      \"description\": \"Commodity description\",\n      \"goods_nomenclature_item_id\":\"123456\",\n" +
                  "\"validity_start_date\": \"2012-01-01T00:00:00.000Z\",\n            \"validity_end_date\": null }\n  }\n}"
              )
            )
        )

        connector.getCommodityCode("123456").futureValue mustBe commodity
      }

      "when validity end date is defined" in {
        val commodity = Commodity(
          "123456",
          "Commodity description",
          Instant.parse("2012-01-01T00:00:00.000Z"),
          Some(Instant.parse("2032-01-01T00:00:00.000Z"))
        )

        wireMockServer.stubFor(
          get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
            .willReturn(
              ok().withBody(
                "{\n  \"data\": {\n    \"attributes\": {\n      \"description\": \"Commodity description\",\n      \"goods_nomenclature_item_id\":\"123456\",\n" +
                  "\"validity_start_date\": \"2012-01-01T00:00:00.000Z\",\n            \"validity_end_date\": \"2032-01-01T00:00:00.000Z\" }\n  }\n}"
              )
            )
        )

        connector.getCommodityCode("123456").futureValue mustBe commodity
      }

    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
          .willReturn(serverError())
      )

      connector.getCommodityCode("123456").failed.futureValue
    }

    "must return a not found future when the server returns a not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
          .willReturn(notFound())
      )

      val connectorFailure = connector.getCommodityCode("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream4xxResponse] mustBe true
    }

    "must return a server error future when ott returns a 5xx status" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
          .willReturn(serverError())
      )

      val connectorFailure = connector.getCommodityCode("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream5xxResponse] mustBe true
    }
  }

  ".getCountries" - {

    "must return countries object" in {

      val body = """{
                   |  "data": [
                   |  {
                   |    "attributes": {
                   |      "id": "CN",
                   |      "description": "China"
                   |     }
                   |   },
                   |   {
                   |     "attributes": {
                   |      "id": "UK",
                   |      "description": "United Kingdom"
                   |     }
                   |    }
                   |  ]
                   |}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/geographical_areas/countries"))
          .willReturn(
            ok().withBody(body)
          )
      )

      val countries = connector.getCountries.futureValue
      countries.size mustEqual 2
      countries.head.id mustEqual "CN"
      countries.head.description mustEqual "China"
      countries(1).id mustEqual "UK"
      countries(1).description mustEqual "United Kingdom"
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/geographical_areas/countries"))
          .willReturn(serverError())
      )

      connector.getCountries.failed.futureValue
    }

    "must return a server error future when ott returns a 5xx status" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/geographical_areas/countries"))
          .willReturn(serverError())
      )

      val connectorFailure = connector.getCountries.failed.futureValue
      connectorFailure.isInstanceOf[Upstream5xxResponse] mustBe true
    }
  }

  ".getCategorisationInfo" - {

    "must return correct OttResponse object" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(
            ok().withBody(
              """{
                |  "data": {
                |    "id": "54267",
                |    "type": "goods_nomenclature",
                |    "attributes": {
                |      "goods_nomenclature_item_id": "9306210000",
                |      "supplementary_measure_unit": "1000 items (1000 p/st)"
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
      connectorResponse.categoryAssessments.head.id mustEqual "238dbab8cc5026c67757c7e05751f312"
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(serverError())
      )

      connector.getCategorisationInfo("123456").failed.futureValue
    }

    "must return a not found future when the server returns a not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(notFound())
      )

      val connectorFailure = connector.getCategorisationInfo("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream4xxResponse] mustEqual true
    }

    "must return a server error future when ott returns a 5xx status" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(serverError())
      )

      val connectorFailure = connector.getCategorisationInfo("123456").failed.futureValue
      connectorFailure.isInstanceOf[Upstream5xxResponse] mustBe true
    }
  }
}
