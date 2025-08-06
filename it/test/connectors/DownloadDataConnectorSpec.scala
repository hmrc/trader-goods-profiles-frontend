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
import com.github.tomakehurst.wiremock.client.WireMock.*
import generators.StatusCodeGenerators
import models.DownloadDataStatus.FileInProgress
import models.{DownloadData, DownloadDataSummary, Email}
import org.apache.pekko.Done
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.test.WireMockSupport
import utils.GetRecordsResponseUtil

import java.time.Instant

class DownloadDataConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GetRecordsResponseUtil
    with OptionValues
    with StatusCodeGenerators {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .configure("features.download-file-enabled" -> true)
      .build()

  private lazy val connector = app.injector.instanceOf[DownloadDataConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val xClientIdName: String  = "X-Client-ID"
  private val xClientId: String      = "tgp-frontend"
  private val downloadDataSummaryUrl =
    s"/trader-goods-profiles-data-store/traders/download-data-summary"

  private val downloadDataUrl =
    s"/trader-goods-profiles-data-store/traders/download-data"

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
  }

  ".requestDownloadData" - {

    "must request download data and return true if successful" in {

      wireMockServer.stubFor(
        post(urlEqualTo(downloadDataUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(status(ACCEPTED))
      )

      connector.requestDownloadData.futureValue mustEqual Done
    }

    ".requestDownloadData" - {

      "must request download data and return Done if successful" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/trader-goods-profiles-data-store/traders/download-data"))
            .withHeader("X-Client-ID", equalTo("tgp-frontend"))
            .willReturn(aResponse().withStatus(202)) // ACCEPTED = 202
        )

        val result = connector.requestDownloadData.futureValue

        result mustBe Done

        wireMockServer.verify(
          postRequestedFor(urlEqualTo("/trader-goods-profiles-data-store/traders/download-data"))
            .withHeader("X-Client-ID", equalTo("tgp-frontend"))
        )
      }

      "must fail if response status is not ACCEPTED" in {
        val errorStatus = 500

        wireMockServer.stubFor(
          post(urlEqualTo("/trader-goods-profiles-data-store/traders/download-data"))
            .withHeader("X-Client-ID", equalTo("tgp-frontend"))
            .willReturn(aResponse().withStatus(errorStatus).withBody("error"))
        )

        val result = connector.requestDownloadData

        val ex = result.failed.futureValue
        ex shouldBe a[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe errorStatus

        wireMockServer.verify(
          postRequestedFor(urlEqualTo("/trader-goods-profiles-data-store/traders/download-data"))
            .withHeader("X-Client-ID", equalTo("tgp-frontend"))
        )
      }
    }


    "must return Seq.empty when response status is unexpected" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(aResponse().withStatus(204))
      )

      connector.getDownloadDataSummary.futureValue mustBe Seq.empty
    }


    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.requestDownloadData.failed.futureValue
    }

    "must return Seq.empty when status is unexpected" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(forbidden())
      )

      connector.getDownloadDataSummary.futureValue mustBe Seq.empty
    }


  }

  ".getDownloadDataSummary" - {

    "must return data when status is OK" in {
      val downloadDataSummary = Seq(DownloadDataSummary("id", testEori, FileInProgress, Instant.now(), Instant.now(), None))

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(ok().withBody(Json.toJson(downloadDataSummary).toString))
      )

      connector.getDownloadDataSummary.futureValue mustBe downloadDataSummary
    }

    "must return empty Seq when status is not OK" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(status(404))
      )

      connector.getDownloadDataSummary.futureValue mustBe Seq.empty
    }

    "must return empty Seq on UpstreamErrorResponse" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(serverError())
      )

      connector.getDownloadDataSummary.futureValue mustBe Seq.empty
    }

    "must return empty Seq when feature flag is disabled" in {
      val appNoDownload = new GuiceApplicationBuilder()
        .configure("features.download-file-enabled" -> false)
        .build()

      val connectorNoDownload = appNoDownload.injector.instanceOf[DownloadDataConnector]

      connectorNoDownload.getDownloadDataSummary.futureValue mustBe Seq.empty
    }
  }

  ".getEmail" - {

    val emailUrl = "/trader-goods-profiles-data-store/traders/email"
    val testEmail = Email("test@example.com", Instant.now())

    "must return Some(email) when status is OK" in {
      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(ok().withBody(Json.toJson(testEmail).toString))
      )

      connector.getEmail.futureValue mustBe Some(testEmail)
    }

    "must return None when status is NOT_FOUND" in {
      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(notFound())
      )

      connector.getEmail.futureValue mustBe None
    }

    "must fail with UpstreamErrorResponse on unexpected error status" in {
      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(status(500).withBody("internal error"))
      )

      val ex = connector.getEmail.failed.futureValue
      ex shouldBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe 500
    }

    "must return None when UpstreamErrorResponse with NOT_FOUND is thrown" in {
      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      connector.getEmail.futureValue mustBe None
    }
  }

  ".updateSeenStatus" - {

    val updateUrl = "/trader-goods-profiles-data-store/traders/download-data-summary"

    "must return Done when response status is NO_CONTENT (204)" in {
      wireMockServer.stubFor(
        patch(urlEqualTo(updateUrl))
          .willReturn(noContent())
      )

      connector.updateSeenStatus.futureValue mustBe Done
    }

    "must fail with UpstreamErrorResponse when response status is not NO_CONTENT" in {
      wireMockServer.stubFor(
        patch(urlEqualTo(updateUrl))
          .willReturn(status(500).withBody("server error"))
      )

      val ex = connector.updateSeenStatus.failed.futureValue
      ex shouldBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe 500
    }
  }


  ".getDownloadData" - {

    "must return data when status is OK" in {
      val downloadData = Seq(DownloadData("downloadURL", "fileName", 600, Seq.empty))

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataUrl))
          .willReturn(ok().withBody(Json.toJson(downloadData).toString))
      )

      connector.getDownloadData.futureValue mustBe downloadData
    }

    "must return empty Seq when status is not OK" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataUrl))
          .willReturn(status(400))
      )

      connector.getDownloadData.futureValue mustBe Seq.empty
    }

    "must return empty Seq on UpstreamErrorResponse" in {
      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataUrl))
          .willReturn(serverError())
      )

      connector.getDownloadData.futureValue mustBe Seq.empty
    }

    "must return empty Seq when feature flag is disabled" in {
      val appNoDownload = new GuiceApplicationBuilder()
        .configure("features.download-file-enabled" -> false)
        .build()

      val connectorNoDownload = appNoDownload.injector.instanceOf[DownloadDataConnector]

      connectorNoDownload.getDownloadData.futureValue mustBe Seq.empty
    }
  }
}
