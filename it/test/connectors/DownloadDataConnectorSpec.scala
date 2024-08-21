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
import models.DownloadDataStatus.FileReady
import models.DownloadDataSummary
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

class DownloadDataConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GetRecordsResponseUtil
    with OptionValues {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[DownloadDataConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val xClientIdName: String  = "X-Client-ID"
  private val xClientId: String      = "tgp-frontend"
  private val downloadDataSummaryUrl =
    s"/trader-goods-profiles-data-store/traders/$testEori/download-data-summary"

  ".requestDownloadData" - {

    "must request to download data and return true if successful" in {

      wireMockServer.stubFor(
        post(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(aResponse().withStatus(ACCEPTED))
      )

      connector.requestDownloadData(testEori).futureValue mustEqual true
    }

    "must request to download data and return false if not successful" in {

      wireMockServer.stubFor(
        post(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector.requestDownloadData(testEori).futureValue mustEqual false
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

  ".getDownloadDataSummary" - {

    val downloadDataSummary = DownloadDataSummary(testEori, FileReady)

    "must get download data summary" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(ok().withBody(Json.toJson(downloadDataSummary).toString))
      )

      connector.getDownloadDataSummary(testEori).futureValue mustBe Some(downloadDataSummary)
    }

    "must return None if Download summary does not exist" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(notFound())
      )

      connector.getDownloadDataSummary(testEori).futureValue mustBe None
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(downloadDataSummaryUrl))
          .withHeader(xClientIdName, equalTo(xClientId))
          .willReturn(serverError())
      )

      connector.getDownloadDataSummary(testEori).failed.futureValue
    }
  }
}
