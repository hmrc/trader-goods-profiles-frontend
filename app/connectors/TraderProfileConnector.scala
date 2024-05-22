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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TraderProfileConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val routerBaseUrl: Service    = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")

  private def submitTraderProfileUrl(eori: String) =
    url"$routerBaseUrl/trader-goods-profiles-router/customs/traders/good-profiles/$eori"

  def submitTraderProfile(traderProfile: TraderProfile, eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(submitTraderProfileUrl(eori))
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .map(_ => Done)

  private def checkTraderProfileUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/tgp/does-profile-exist/$eori"

  def checkTraderProfile(eori: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .get(checkTraderProfileUrl(eori))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => true
        }
      }
      .recoverWith { case e: NotFoundException =>
        Future.successful(false)
      }

  private def getTraderProfileUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/tgp/get-profile/$eori"

  def getTraderProfile(eori: String)(implicit hc: HeaderCarrier): Future[TraderProfile] =
    httpClient
      .get(getTraderProfileUrl(eori))
      .execute[HttpResponse]
      .map(response => response.json.as[TraderProfile])
}
