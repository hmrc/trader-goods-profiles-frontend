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
import models.{HistoricProfileData, LegacyRawReads, TraderProfile}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TraderProfileConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends LegacyRawReads {

  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val routerUrl: Service        = config.get[Service]("microservice.services.trader-goods-profiles-router")

  private val traderProfileUrl =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/profile"

  def submitTraderProfile(traderProfile: TraderProfile)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(traderProfileUrl)
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .flatMap(response =>
        response.status match {
          case OK => Future.successful(Done)
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      )

  def checkTraderProfile(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .head(traderProfileUrl)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(true)
          case NOT_FOUND => Future.successful(false)
          case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
      .recover {
        case x: UpstreamErrorResponse if x.statusCode == NOT_FOUND =>
          false
      }

  private def getTraderProfileUrl =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/customs/traders/goods-profiles"

  def getTraderProfile(implicit hc: HeaderCarrier): Future[TraderProfile] =
    httpClient
      .get(getTraderProfileUrl)
      .execute[TraderProfile]

  def getHistoricProfileData(eori: String)(implicit hc: HeaderCarrier): Future[Option[HistoricProfileData]] = {
    val profileUrl = url"$routerUrl/trader-goods-profiles-router/customs/traders/goods-profiles/$eori"

    httpClient
      .get(profileUrl)
      .setHeader(("Accept", "application/vnd.hmrc.1.0+json"))
      .execute[HistoricProfileData]
      .map(Some(_))
      .recover {
        case UpstreamErrorResponse(_, FORBIDDEN, _, _) => None
        case UpstreamErrorResponse(message, BAD_REQUEST, _, _) if message.contains("007") => None
      }

  }
}
