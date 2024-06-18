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

import models.audits.OttAuditData
import models.ott.response.{CountriesResponse, OttResponse}
import models.{Commodity, Country}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsResult, Reads}
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, NotFoundException, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (config: Configuration, httpClient: HttpClientV2, auditService: AuditService)(implicit
  ec: ExecutionContext
) {

  private val baseUrl: String                          = config.get[String]("microservice.services.online-trade-tariff-api.url")
  private def ottCommoditiesUrl(commodityCode: String) =
    url"$baseUrl/ott/commodities/$commodityCode"

  private def ottGreenLanesUrl(commodityCode: String) =
    url"$baseUrl/ott/goods-nomenclatures/$commodityCode"

  private def ottCountriesUrl =
    url"$baseUrl/xi/api/v2/geographical_areas/countries"

  private def getFromOtt[T](
    url: URL,
    authToken: String,
    auditDetails: Option[OttAuditData],
    auditFunction: (Option[OttAuditData], Instant, Instant, Int, Option[String], Option[T]) => Future[Done]
  )(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] = {
    val newHeaderCarrier = hc.copy(authorization = Some(Authorization(authToken)))

    val requestStartTime = Instant.now

    httpClient
      .get(url)(newHeaderCarrier)
      .execute[HttpResponse]
      .flatMap { response =>
        val requestEndTime = Instant.now

        response.json
          .validate[T]
          .map { result =>
            auditFunction.apply(
              auditDetails,
              requestStartTime,
              requestEndTime,
              response.status,
              None,
              Some(result)
            )
            Future.successful(result)
          }
          .recoverTotal(error => Future.failed(JsResult.Exception(error)))
      }
      .recoverWith {
        case e: NotFoundException =>
          auditFunction.apply(auditDetails, requestStartTime, Instant.now, e.responseCode, Some(e.message), None)

          Future.failed(UpstreamErrorResponse(e.message, NOT_FOUND))

        case e: UpstreamErrorResponse =>
          auditFunction.apply(auditDetails, requestStartTime, Instant.now, e.statusCode, Some(e.message), None)
          Future.failed(e)

        case e: Exception =>
          //E.g. Any error not directly related to the http call, e.g. Json parsing
          auditFunction.apply(auditDetails, requestStartTime, Instant.now, OK, Some(e.getMessage), None)
          Future.failed(e)

      }
  }

  private def getFromOttWithCommodityCode[T](
    commodityCode: String,
    urlFunc: String => URL,
    authToken: String,
    auditDetails: Option[OttAuditData],
    auditFunction: (Option[OttAuditData], Instant, Instant, Int, Option[String], Option[T]) => Future[Done]
  )(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] =
    getFromOtt(urlFunc(commodityCode), authToken, auditDetails, auditFunction)

  def getCommodityCode(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    journey: String,
    recordId: Option[String]
  )(implicit hc: HeaderCarrier): Future[Commodity] =
    getFromOttWithCommodityCode[Commodity](
      commodityCode,
      ottCommoditiesUrl,
      "bearerToken",
      Some(OttAuditData(eori, affinityGroup, recordId, commodityCode, None, None, Some(journey))),
      auditService.auditValidateCommodityCode
    )

  def getCategorisationInfo(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: Option[String],
    countryOfOrigin: String,
    dateOfTrade: LocalDate
  )(implicit hc: HeaderCarrier): Future[OttResponse]                 =
    getFromOttWithCommodityCode[OttResponse](
      commodityCode,
      ottGreenLanesUrl,
      "bearerToken",
      Some(OttAuditData(eori, affinityGroup, recordId, commodityCode, Some(countryOfOrigin), Some(dateOfTrade), None)),
      auditService.auditGetCategorisationAssessmentDetails
    )
  def getCountries(implicit hc: HeaderCarrier): Future[Seq[Country]] =
    getFromOtt[CountriesResponse](
      ottCountriesUrl,
      "bearerToken",
None,
(_, _, _, _, _, _) => Future.successful(Done)
    ).map(_.data)
}
