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
import models.{Commodity, TraderProfile}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrl: Service              = config.get[Service]("microservice.services.online-trade-tariff-api")
  private def ottUrl(commodityCode: String) =
    url"$baseUrl/ott/commodities/$commodityCode"

  def getCommodityCode(commodityCode: String)(implicit hc: HeaderCarrier): Future[Commodity] =
    httpClient
      .get(ottUrl(commodityCode))
      .execute[HttpResponse]
      .map(res => res.json.as[Commodity])
}
