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
import models.TraderProfile
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetGoodsRecordsConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private def getGoodsRecordsUrl(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )                                     =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[TraderProfile] =
    httpClient
      .get(getGoodsRecordsUrl(eori))
      .execute[HttpResponse]
      .map(response => response.json.as[TraderProfile])
}
