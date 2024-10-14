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
import com.github.tomakehurst.wiremock.client.WireMock._
import generators.StatusCodeGenerators
import models.DownloadDataStatus.RequestFile
import models.{DownloadData, DownloadDataSummary, Email}
import org.apache.pekko.Done
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
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

  private val xClientIdName: String = "X-Client-ID"
  private val xClientId: String = "tgp-frontend"
  private val downloadDataSummaryUrl =
    s"/trader-goods-profiles-data-store/traders/$testEori/download-data-summary"

  private val downloadDataUrl =
    s"/trader-goods-profiles-data-store/traders/$testEori/download-data"

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

      connector.requestDownloadData(testEori).futureValue mustEqual Done
    }

    "must return a failed future when anything but Accepted is returned" in {

        wireMockServer.stubFor(
          post(urlEqualTo(downloadDataUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(status(errorResponses.sample.value))
        )

        val result = connector.requestDownloadData(testEori)

        result.failed.futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.requestDownloadData(testEori).failed.futureValue
    }

  }

  ".getDownloadData" - {

    "must get download data data" in {

      val downloadURL = "downloadURL"
      val filename = "filename"
      val filesize = 600
      val metadata = Seq.empty
      val downloadData = DownloadData(downloadURL, filename, filesize, metadata)

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataUrl))
          .willReturn(ok().withBody(Json.toJson(downloadData).toString))
      )

      connector.getDownloadData(testEori).futureValue mustBe Some(downloadData)
    }

    "must return None if Download summary does not exist" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataUrl))
          .willReturn(notFound())
      )

      connector.getDownloadData(testEori).futureValue mustBe None
    }

    "must return none when status code is anything but Ok" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(serverError())
      )

      connector.getDownloadData(testEori).futureValue mustBe None
    }
  }

  ".getDownloadDataSummary" - {

    val downloadDataSummary = DownloadDataSummary(testEori, RequestFile, None)

    "must get download data summary" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .willReturn(ok().withBody(Json.toJson(downloadDataSummary).toString))
      )

      connector.getDownloadDataSummary(testEori).futureValue mustBe Some(downloadDataSummary)
    }

    "must return None" - {
      "if status is anything but a Ok response" in {
          wireMockServer.stubFor(
            get(urlEqualTo(downloadDataSummaryUrl))
              .willReturn(status(errorResponses.sample.value))
          )

          connector.getDownloadDataSummary(testEori).futureValue mustBe None
      }

      "if Download summary does not exist" in {

        wireMockServer.stubFor(
          get(urlEqualTo(downloadDataSummaryUrl))
            .willReturn(notFound())
        )

        connector.getDownloadDataSummary(testEori).futureValue mustBe None
      }

      "if feature flag for downloading data is disabled" in {

        val appNoDownload = new GuiceApplicationBuilder()
          .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
          .configure("features.download-file-enabled" -> false)
          .build()

        val connectorNoDownload = appNoDownload.injector.instanceOf[DownloadDataConnector]

        wireMockServer.stubFor(
          get(urlEqualTo(downloadDataSummaryUrl))
            .withHeader(xClientIdName, equalTo(xClientId))
            .willReturn(ok().withBody(Json.toJson(downloadDataSummary).toString))
        )

        connectorNoDownload.getDownloadDataSummary(testEori).futureValue mustBe None
      }

    }
  }

  ".getEmail" - {
    val emailUrl =
      s"/trader-goods-profiles-data-store/traders/$testEori/email"

    val address = "somebody@email.com"
    val timestamp = Instant.now
    val email = Email(address, timestamp)
    "must get email" in {

      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(ok().withBody(Json.toJson(email).toString))
      )

      connector.getEmail(testEori).futureValue mustBe email
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(emailUrl))
          .willReturn(serverError())
      )

      connector.getEmail(testEori).failed.futureValue
    }

    "must return a failed future when anything but Ok is returned" in {

      wireMockServer.stubFor(
          get(urlEqualTo(emailUrl))
            .willReturn(status(errorResponses.sample.value))
        )

      val result = connector.requestDownloadData(testEori)

      result.failed.futureValue
    }
  }

}
