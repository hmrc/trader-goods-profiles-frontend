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

import models.audits.{AuditGetCategorisationAssessment, AuditValidateCommodityCode, OttAuditData}
import models.helper.Journey
import models.ott.response.{CountriesResponse, OttResponse}
import models.{Commodity, Country}
import play.api.Configuration
import play.api.libs.json.{JsResult, Reads}
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (config: Configuration, httpClient: HttpClientV2, auditService: AuditService)(implicit
  ec: ExecutionContext
) {

  private val baseUrl: String   = config.get[String]("microservice.services.online-trade-tariff-api.url")
  private val authToken: String = config.get[String]("microservice.services.online-trade-tariff-api.bearerToken")

  private def ottCommoditiesUrl(commodityCode: String) =
    url"$baseUrl/xi/api/v2/commodities/$commodityCode"

  private def ottGreenLanesUrl(commodityCode: String) =
    url"$baseUrl/xi/api/v2/green_lanes/goods_nomenclatures/$commodityCode"

  private def ottCountriesUrl =
    url"$baseUrl/xi/api/v2/geographical_areas/countries"

  private def getFromOtt[T](
    url: URL,
    auditDetails: Option[OttAuditData]
  )(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] = {
    val requestStartTime = Instant.now

    httpClient
      .get(url)(hc)
      .setHeader(HeaderNames.authorisation -> s"Token $authToken")
      .withProxy
      .execute[HttpResponse]
      .flatMap { response =>
        val requestEndTime = Instant.now

        response.json
          .validate[T]
          .map { result =>
            auditService.auditOttCall(
              auditDetails,
              requestStartTime,
              requestEndTime,
              response.status,
              None,
              Some(result)
            )
            Future.successful(result)
          }
          .recoverTotal { error =>
            auditService.auditOttCall(
              auditDetails,
              requestStartTime,
              Instant.now,
              response.status,
              Some(error.errors.toString()),
              None
            )

            Future.failed(JsResult.Exception(error))
          }
      }
      .recoverWith {
        case e: HttpException =>
          auditService.auditOttCall(auditDetails, requestStartTime, Instant.now, e.responseCode, Some(e.message), None)

          Future.failed(UpstreamErrorResponse(e.message, e.responseCode))

        case e: UpstreamErrorResponse =>
          auditService.auditOttCall(auditDetails, requestStartTime, Instant.now, e.statusCode, Some(e.message), None)
          Future.failed(e)

      }
  }

  def getCommodityCode(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    recordId: Option[String]
  )(implicit hc: HeaderCarrier): Future[Commodity] = {

    val auditDetails = OttAuditData(
      AuditValidateCommodityCode,
      eori,
      affinityGroup,
      recordId,
      commodityCode,
      None,
      None,
      Some(journey)
    )

    getFromOtt[Commodity](
      ottCommoditiesUrl(commodityCode),
      Some(auditDetails)
    )
  }

  def getCategorisationInfo(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: Option[String],
    countryOfOrigin: String,
    dateOfTrade: LocalDate
  )(implicit hc: HeaderCarrier): Future[OttResponse] = {
    val auditDetails = OttAuditData(
      AuditGetCategorisationAssessment,
      eori,
      affinityGroup,
      recordId,
      commodityCode,
      Some(countryOfOrigin),
      Some(dateOfTrade),
      None
    )

    getFromOtt[OttResponse](
      ottGreenLanesUrl(commodityCode),
      Some(auditDetails)
    )
  }

  def getCountries(implicit hc: HeaderCarrier): Future[Seq[Country]] =
    getFromOtt[CountriesResponse](
      ottCountriesUrl,
      None
    ).map(_.data)
}
