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
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{EmailNotification, HistoricProfileData, TraderProfile}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import testModels.DataStoreProfile
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class NotificationConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-data-store.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[NotificationConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".submitNotification" - {

    val testExpiredDate   = "05 Sept 2024"
    val emailNotification = EmailNotification(testExpiredDate)

    "must submit a notification" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId/notification"))
          .withRequestBody(equalTo(Json.toJson(emailNotification).toString))
          .willReturn(ok())
      )

      connector.submitNotification(testEori, testRecordId, emailNotification).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-data-store/traders/$testEori/records/$testRecordId/notification"))
          .withRequestBody(equalTo(Json.toJson(emailNotification).toString))
          .willReturn(serverError())
      )

      connector.submitNotification(testEori, testRecordId, emailNotification).failed.futureValue
    }
  }
}
