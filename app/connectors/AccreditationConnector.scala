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

import config.Service
import models.{AdviceRequest, LegacyRawReads}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

class AccreditationConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends LegacyRawReads {

  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")

  private val clientIdAndAcceptHeaders           =
    Seq("X-Client-ID" -> "tgp-frontend", "Accept" -> "application/vnd.hmrc.1.0+json")

  private def accreditationUrl(recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/$recordId/advice"

  def submitRequestAccreditation(adviceRequest: AdviceRequest)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(accreditationUrl(adviceRequest.recordId))
      .setHeader(clientIdAndAcceptHeaders: _*)
      .withBody(Json.toJson(adviceRequest))
      .execute[HttpResponse]
      .map(_ => Done)

  def withdrawRequestAccreditation(recordId: String, withdrawReason: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .put(accreditationUrl(recordId))
      .setHeader(clientIdAndAcceptHeaders: _*)
      .withBody(Json.obj("withdrawReason" -> withdrawReason))
      .execute[HttpResponse]
      .map(_ => Done)
}
