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
import models.{HistoricProfileData, TraderProfile}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

class TraderProfileConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val routerUrl: Service        = config.get[Service]("microservice.services.trader-goods-profiles-router")

  private def traderProfileUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/profile"

  def submitTraderProfile(traderProfile: TraderProfile, eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(traderProfileUrl(eori))
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .map(_ => Done)

  def checkTraderProfile(eori: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .head(traderProfileUrl(eori))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK        => true
          case NOT_FOUND => false
        }
      }
      .recover { case _: NotFoundException =>
        false
      }

  private def getTraderProfileUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/customs/traders/goods-profiles/$eori"

  def getTraderProfile(eori: String)(implicit hc: HeaderCarrier): Future[TraderProfile] =
    httpClient
      .get(getTraderProfileUrl(eori))
      .execute[TraderProfile]

  def getHistoricProfileData(eori: String)(implicit hc: HeaderCarrier): Future[Option[HistoricProfileData]] = {
    val profileUrl = url"$routerUrl/trader-goods-profiles-router/customs/traders/goods-profiles/$eori"

    httpClient
      .get(profileUrl)
      .setHeader(("Accept", "application/vnd.hmrc.1.0+json"))
      .execute[HistoricProfileData]
      .map(Some(_))
      .recover { case Upstream4xxResponse(_, FORBIDDEN, _, _) =>
        None
      }

  }
}
