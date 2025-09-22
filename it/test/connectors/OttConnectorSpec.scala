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

import base.TestConstants.{testEori, testRecordId}
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.{Commodity, Country, CountryCodeCache}
import models.helper.CreateRecordJourney
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.http.Status.IM_A_TEAPOT
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import repositories.CountryRepository

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class OttConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private val auditService        = mock[AuditService]
  private val mockCacheRepository = mock[CountryRepository]

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.online-trade-tariff-api.url" -> wireMockUrl)
      .overrides(
        bind[AuditService].toInstance(auditService),
        bind[CountryRepository].toInstance(mockCacheRepository)
      )
      .build()

  private lazy val connector = app.injector.instanceOf[OttConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    reset(auditService)
    reset(mockCacheRepository)

    when(auditService.auditOttCall(any(), any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Done))

    super.beforeEach()
  }

  "headers" - {
    "must return x-api-key header" in {
      val appWithFeatureEnabled = new GuiceApplicationBuilder()
        .build()

      val connectorWithFeatureEnabled = appWithFeatureEnabled.injector.instanceOf[OttConnector]

      connectorWithFeatureEnabled.headers mustBe "x-api-key" -> "apiKey"
    }
  }

  ".getCommodityCode" - {

    "must return correct commodity object" - {

      "when validity end date is undefined" in {
        val commodity =
          Commodity(
            "9306210000",
            List("test_description"),
            Instant.parse("2012-01-01T00:00:00.000Z"),
            None
          )

        stubGreenLanes(excludeEndDate = true)

        connector
          .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
          .futureValue mustBe commodity

        withClue("must have audited the request") {
          verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
        }

      }

      "when validity end date is defined" in {
        val commodity = Commodity(
          "9306210000",
          List("test_description"),
          Instant.parse("2012-01-01T00:00:00.000Z"),
          Some(Instant.parse("2032-01-01T00:00:00.000Z"))
        )

        stubGreenLanes()

        connector
          .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
          .futureValue mustBe commodity

        withClue("must have audited the request") {
          verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
        }
      }

    }

    "must return a not found future when the server returns a not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/commodities/123456"))
          .willReturn(notFound())
      )

      val connectorFailure = connector
        .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
        .failed
        .futureValue
      connectorFailure.isInstanceOf[UpstreamErrorResponse] mustBe true

      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
      }
    }

    "must return a server error future when ott returns a 5xx status" in {

      wireMockServer.stubFor(
        get(urlMatching("/xi/api/v2/green_lanes/goods_nomenclatures/123456(\\?.*)?"))
          .willReturn(serverError())
      )

      val connectorFailure = connector
        .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
        .failed
        .futureValue
      connectorFailure.isInstanceOf[UpstreamErrorResponse] mustBe true

      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
      }
    }

    "must return a failed future when the server returns any other error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(status(IM_A_TEAPOT))
      )

      connector
        .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
        .failed
        .futureValue

      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
      }
    }

    "must return an exception future when json cannot be parsed at all" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(
            ok().withBody(
              "{}"
            )
          )
      )

      val connectorFailure = connector
        .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
        .failed
        .futureValue
      connectorFailure.isInstanceOf[Exception] mustBe true

      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
      }
    }

    "must return an Js exception future when json is parsable but does not match Commodity format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(
            ok().withBody(
              "{\n \"description\": \"Commodity description\"\n}"
            )
          )
      )

      val connectorFailure = connector
        .getCommodityCode("123456", testEori, AffinityGroup.Individual, CreateRecordJourney, "CX", None)
        .failed
        .futureValue
      connectorFailure.isInstanceOf[Exception] mustBe true

      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any, any, any, any, any, any)(any)
      }
    }

  }

  ".getCountries" - {
    "when cache is available" - {
      "must return cached countries without calling the API" in {
        val cachedCountries = Seq(Country("CN", "China"), Country("UK", "United Kingdom"))
        when(mockCacheRepository.get())
          .thenReturn(Future.successful(Some(CountryCodeCache("ott_country_codes", cachedCountries, Instant.now()))))
        val result          = connector.getCountries.futureValue
        result mustBe cachedCountries.sortWith(_.description < _.description)
        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/xi/api/v2/geographical_areas/countries")))
      }
    }

    "when cache is not available" - {
      "must call the API and cache the response" in {
        val apiCountries = Seq(Country("CN", "China"), Country("UK", "United Kingdom"))
        val jsonResponse = Json
          .obj(
            "data" -> Json.arr(
              Json.obj("attributes" -> Json.obj("id" -> "CN", "description" -> "China")),
              Json.obj("attributes" -> Json.obj("id" -> "UK", "description" -> "United Kingdom"))
            )
          )
          .toString()

        wireMockServer.stubFor(
          get(urlEqualTo("/xi/api/v2/geographical_areas/countries"))
            .willReturn(ok().withBody(jsonResponse))
        )
        when(mockCacheRepository.get()).thenReturn(Future.successful(None))
        when(mockCacheRepository.set(any())).thenReturn(Future.successful(()))
        val result = connector.getCountries.futureValue
        result mustBe apiCountries.sortWith(_.description < _.description)
        verify(mockCacheRepository).set(apiCountries)
      }

      "must return a failed future when the API returns an error" in {
        wireMockServer.stubFor(
          get(urlEqualTo("/xi/api/v2/geographical_areas/countries"))
            .willReturn(serverError())
        )
        when(mockCacheRepository.get()).thenReturn(Future.successful(None))
        val result = connector.getCountries.failed.futureValue
        result mustBe a[UpstreamErrorResponse]
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "must correctly parse and sort countries from the API" in {
      val jsonResponse = Json
        .obj(
          "data" -> Json.arr(
            Json.obj("attributes" -> Json.obj("id" -> "UK", "description" -> "United Kingdom")),
            Json.obj("attributes" -> Json.obj("id" -> "CN", "description" -> "China"))
          )
        )
        .toString()

      wireMockServer.stubFor(
        get(urlEqualTo("/xi/api/v2/geographical_areas/countries"))
          .willReturn(ok().withBody(jsonResponse))
      )
      when(mockCacheRepository.get()).thenReturn(Future.successful(None))
      when(mockCacheRepository.set(any())).thenReturn(Future.successful(()))
      val result = connector.getCountries.futureValue
      result mustBe Seq(Country("CN", "China"), Country("UK", "United Kingdom"))
    }

    "must return a server error future when ott returns a 5xx status" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/xi/api/v2/geographical_areas/countries"))
          .willReturn(serverError())
      )
      when(mockCacheRepository.get()).thenReturn(Future.successful(None))
      val connectorFailure = connector.getCountries.failed.futureValue
      connectorFailure.isInstanceOf[UpstreamErrorResponse] mustBe true
      verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
    }
  }

  ".getCategorisationInfo" - {
    "must return correct OttResponse object" in {
      stubGreenLanes()
      val connectorResponse = connector
        .getCategorisationInfo("123456", testEori, AffinityGroup.Individual, Some(testRecordId), "CX", LocalDate.now())
        .futureValue
      connectorResponse.categoryAssessments.size mustEqual 1
      connectorResponse.categoryAssessments.head.id mustEqual "238dbab8cc5026c67757c7e05751f312"
      connectorResponse.otherExemptions.size mustEqual 1
      connectorResponse.otherExemptions.head.code mustEqual "WFE013"
      connectorResponse.descendents.size mustEqual 3
      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "must return a not found future when the server returns a not found" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456"))
          .willReturn(notFound())
      )
      val connectorFailure = connector
        .getCategorisationInfo("123456", testEori, AffinityGroup.Individual, Some(testRecordId), "CX", LocalDate.now())
        .failed
        .futureValue
      connectorFailure.isInstanceOf[UpstreamErrorResponse] mustEqual true
      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "must return a server error future when ott returns a 5xx status" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/xi/api/v2/green_lanes/goods_nomenclatures/123456?filter%5Bgeographical_area_id%5D=CX"))
          .willReturn(serverError())
      )
      val connectorFailure = connector
        .getCategorisationInfo("123456", testEori, AffinityGroup.Individual, Some(testRecordId), "CX", LocalDate.now())
        .failed
        .futureValue
      connectorFailure.isInstanceOf[UpstreamErrorResponse] mustBe true
      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "must return a failed future when the server returns any other error" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/goods-nomenclatures/123456"))
          .willReturn(serverError())
      )
      connector
        .getCategorisationInfo("123456", testEori, AffinityGroup.Individual, Some(testRecordId), "CX", LocalDate.now())
        .failed
        .futureValue
      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }

    "must return a Js exception future when json cannot be parsed" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/ott/goods-nomenclatures/123456"))
          .willReturn(
            ok().withBody(
              "{\n \"description\": \"Commodity description\",\n}"
            )
          )
      )
      val connectorFailure = connector
        .getCategorisationInfo("123456", testEori, AffinityGroup.Individual, Some(testRecordId), "CX", LocalDate.now())
        .failed
        .futureValue
      connectorFailure.isInstanceOf[Exception] mustBe true
      withClue("must have audited the request") {
        verify(auditService).auditOttCall(any(), any(), any(), any(), any(), any())(any())
      }
    }
  }

  private def stubGreenLanes(excludeEndDate: Boolean = false) = {
    val validityEndDateField = if (!excludeEndDate) {
      ",\"validity_end_date\": \"2032-01-01T00:00:00.000Z\""
    } else {
      ""
    }
    wireMockServer.stubFor(
      get(urlMatching("/xi/api/v2/green_lanes/goods_nomenclatures/123456(\\?.*)?"))
        .willReturn(
          ok().withBody(
            s"""{
               |  "data": {
               |    "id": "54267",
               |    "type": "goods_nomenclature",
               |    "attributes": {
               |      "goods_nomenclature_item_id": "9306210000",
               |      "supplementary_measure_unit": "1000 items (1000 p/st)",
               |      "description": "test_description",
               |      "validity_start_date": "2012-01-01T00:00:00.000Z"
               |      $validityEndDateField
               |    },
               |    "relationships": {
               |      "applicable_category_assessments": {
               |        "data": [
               |          {
               |            "id": "238dbab8cc5026c67757c7e05751f312",
               |            "type": "category_assessment"
               |          }
               |        ]
               |      },
               |      "descendant_category_assessments": {
               |        "data": []
               |      },
               |      "descendants": {
               |        "data": [
               |          {
               |            "id": "72785",
               |            "type": "goods_nomenclature"
               |          },
               |          {
               |            "id": "94337",
               |            "type": "goods_nomenclature"
               |          },
               |          {
               |            "id": "94338",
               |            "type": "goods_nomenclature"
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
               |            },
               |            {
               |              "id": "WFE013",
               |              "type": "exemption"
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
               |          "data": []
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
               |    },
               |    {
               |      "id": "WFE013",
               |      "type": "exemption",
               |      "attributes": {
               |         "code": "WFE013",
               |         "description": "NIRMS Exemption",
               |         "formatted_description": "NIRMS Exemption"
               |         }
               |    }
               |  ]
               |}""".stripMargin
          )
        )
    )
  }

}
