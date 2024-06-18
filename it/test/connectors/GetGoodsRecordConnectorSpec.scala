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

import base.TestConstants.{lastUpdatedDate, page, recordsize, testEori}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.router.responses.GetRecordsResponse
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import utils.GetRecordsResponseUtil

class GetGoodsRecordConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with GetRecordsResponseUtil {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[GetGoodsRecordsConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def getGoodsRecordsUrl =
    s"/trader-goods-profiles-data-store/traders/$testEori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$recordsize"

  ".getRecords" - {

    "must get goods records" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getGoodsRecordsUrl))
          .willReturn(ok().withBody(getMultipleRecordResponseData.toString()))
      )

      connector
        .getRecords(testEori, Some(lastUpdatedDate), Some(page), Some(recordsize))
        .futureValue mustBe getMultipleRecordResponseData.as[GetRecordsResponse]
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer
        .stubFor(
          get(urlEqualTo(getGoodsRecordsUrl))
            .willReturn(serverError())
        )
      connector.getRecords(testEori, Some(lastUpdatedDate), Some(page), Some(recordsize)).failed.futureValue
    }

    "must return a failed future when the json does not match the format" in {

      wireMockServer.stubFor(
        get(urlEqualTo(getGoodsRecordsUrl))
          .willReturn(ok().withBody("{'eori': '123', 'commodity': '10410100'}"))
      )

      connector.getRecords(testEori, Some(lastUpdatedDate), Some(page), Some(recordsize)).failed.futureValue
    }
  }

}
