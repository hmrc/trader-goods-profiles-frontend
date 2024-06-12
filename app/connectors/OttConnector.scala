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
import models.{Commodity, Countries}
import models.ott.response.OttResponse
import play.api.Configuration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsResult, Reads}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, NotFoundException, StringContextOps, Upstream5xxResponse, UpstreamErrorResponse}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrl: Service                         = config.get[Service]("microservice.services.online-trade-tariff-api")
  private def ottCommoditiesUrl(commodityCode: String) =
    url"$baseUrl/ott/commodities/$commodityCode"

  private def ottGreenLanesUrl(commodityCode: String) =
    url"$baseUrl/ott/goods-nomenclatures/$commodityCode"

  private def ottCountriesUrl =
    url"$baseUrl/ott/geographical_areas/countries"

  private def getFromOtt[T](url: URL, authToken: String)(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] = {
    val newHeaderCarrier = hc.copy(authorization = Some(Authorization(authToken)))

    httpClient
      .get(url)(newHeaderCarrier)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            response.json
              .validate[T]
              .map(result => Future.successful(result))
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
        }
      }
      .recoverWith {
        case e: NotFoundException   =>
          Future.failed(UpstreamErrorResponse(e.message, NOT_FOUND))
        case e: Upstream5xxResponse =>
          Future.failed(UpstreamErrorResponse(e.message, INTERNAL_SERVER_ERROR))
      }
  }

  private def getFromOttWithCommodityCode[T](commodityCode: String, urlFunc: String => URL, authToken: String)(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] =
    getFromOtt(urlFunc(commodityCode), authToken)

  def getCommodityCode(commodityCode: String)(implicit hc: HeaderCarrier): Future[Commodity] =
    getFromOttWithCommodityCode[Commodity](commodityCode, ottCommoditiesUrl, "bearerToken")

  def getCategorisationInfo(commodityCode: String)(implicit hc: HeaderCarrier): Future[OttResponse] =
    getFromOttWithCommodityCode[OttResponse](commodityCode, ottGreenLanesUrl, "bearerToken")

  def getCountries(implicit hc: HeaderCarrier): Future[Countries] =
    getFromOtt[Countries](ottCountriesUrl, "bearerToken")
}
