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
import models.{Commodity, Country, LegacyRawReads}
import play.api.Configuration
import play.api.libs.json.{JsResult, Reads}
import repositories.CountryRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (
                               config: Configuration,
                               httpClient: HttpClientV2,
                               auditService: AuditService,
                               cacheRepository: CountryRepository
                             )(implicit ec: ExecutionContext) extends LegacyRawReads {

  private val useAPIKeyFeature: Boolean = config.get[Boolean]("features.online-trade-tariff-useApiKey")

  private val baseUrl: String = config.get[String]("microservice.services.online-trade-tariff-api.url")
  private val authToken: String = config.get[String]("microservice.services.online-trade-tariff-api.bearerToken")
  private val apiKey: String = config.get[String]("microservice.services.online-trade-tariff-api.apiKey")
  private val useProxy: Boolean = config.get[Boolean]("microservice.services.online-trade-tariff-api.useProxy")

  val headers: (String, String) =
    if (useAPIKeyFeature) "x-api-key" -> s"$apiKey" else HeaderNames.authorisation -> s"Token $authToken"

  private def ottGreenLanesUrl(commodityCode: String, queryParams: Map[String, String]) =
    url"$baseUrl/xi/api/v2/green_lanes/goods_nomenclatures/$commodityCode?$queryParams"

  private def ottCountriesUrl =
    url"$baseUrl/xi/api/v2/geographical_areas/countries"

  private val cacheKey = "ott_country_codes"

  private def getFromOtt[T](
                             url: URL,
                             auditDetails: Option[OttAuditData]
                           )(implicit hc: HeaderCarrier, reads: Reads[T]): Future[T] = {
    val requestStartTime = Instant.now

    val request = httpClient
      .get(url)(hc)
      .setHeader(headers)

    val updatedRequest = if (useProxy) request.withProxy else request

    updatedRequest
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
                        countryOfOrigin: String,
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

    val queryParams: Map[String, String] =
      Map("filter[geographical_area_id]" -> countryOfOrigin)

    for {
      ottResponse <- getFromOtt[OttResponse](
        ottGreenLanesUrl(commodityCode, queryParams),
        Some(auditDetails)
      )
    } yield Commodity(
      commodityCode = ottResponse.goodsNomenclature.commodityCode,
      descriptions = ottResponse.goodsNomenclature.descriptions,
      validityStartDate = ottResponse.goodsNomenclature.validityStartDate,
      validityEndDate = ottResponse.goodsNomenclature.validityEndDate
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

    val queryParams = Map(
      "filter[geographical_area_id]" -> countryOfOrigin
    )

    getFromOtt[OttResponse](
      ottGreenLanesUrl(commodityCode, queryParams),
      Some(auditDetails)
    )
  }

  def getCountriesApiCall(implicit hc: HeaderCarrier): Future[Seq[Country]] =
    getFromOtt[CountriesResponse](ottCountriesUrl, None).map(_.data.sortWith(_.description < _.description))

  def getCountries(implicit hc: HeaderCarrier): Future[Seq[Country]] = {
    val requestDateTime = Instant.now()
    cacheRepository.get().flatMap {
      case Some(cache) =>
        auditService.auditOttCall(
          auditDetails = None,
          requestDateTime = requestDateTime,
          responseDateTime = Instant.now(),
          responseStatus = 200,
          errorMessage = Some("Retrieved from cache"),
          response = Some(cache.data)
        ).map(_ => cache.data.sortWith(_.description < _.description))
      case None =>
        getCountriesApiCall(hc).flatMap { countries =>
          cacheRepository.set(countries).flatMap { _ =>
            auditService.auditOttCall(
              auditDetails = None,
              requestDateTime = requestDateTime,
              responseDateTime = Instant.now(),
              responseStatus = 200,
              errorMessage = None,
              response = Some(countries)
            ).map(_ => countries)
          }
        }.recover {
          case e: Throwable =>
            auditService.auditOttCall(
              auditDetails = None,
              requestDateTime = requestDateTime,
              responseDateTime = Instant.now(),
              responseStatus = 500,
              errorMessage = Some(s"API unavailable and no cache: ${e.getMessage}"),
              response = None
            )
            println(s"API error: ${e.getMessage}")
            Seq.empty[Country]
        }
    }
  }
}
