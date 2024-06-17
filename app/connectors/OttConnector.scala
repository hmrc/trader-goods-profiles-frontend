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

import com.fasterxml.jackson.core.JsonParseException
import config.Service
import models.Commodity
import models.audits.OttAuditData
import models.ott.response.OttResponse
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsResult, Reads}
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpException, HttpResponse, NotFoundException, StringContextOps, Upstream5xxResponse, UpstreamErrorResponse}
import utils.HttpStatusCodeDescriptions.codeDescriptions

import java.net.URL
import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OttConnector @Inject() (config: Configuration, httpClient: HttpClientV2, auditService: AuditService)(implicit
  ec: ExecutionContext
) {

  private val baseUrl: Service                         = config.get[Service]("microservice.services.online-trade-tariff-api")
  private def ottCommoditiesUrl(commodityCode: String) =
    url"$baseUrl/ott/commodities/$commodityCode"

  private def ottGreenLanesUrl(commodityCode: String) =
    url"$baseUrl/ott/goods-nomenclatures/$commodityCode"

  private def getFromOtt[T](
    commodityCode: String,
    urlFunc: String => URL,
    authToken: String,
    auditDetails: OttAuditData,
    auditFunction: (OttAuditData, Instant, Instant, Int, String, Option[T]) => Future[Done]
  )(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] = {
    val newHeaderCarrier = hc.copy(authorization = Some(Authorization(authToken)))

    val requestStartTime        = Instant.now
    val x: Future[HttpResponse] = httpClient
      .get(urlFunc(commodityCode))(newHeaderCarrier)
      .execute[HttpResponse]

    x.flatMap { response =>
      val requestEndTime = Instant.now

      response.status match {
        case OK =>
          response.json
            .validate[T]
            .map { result =>
              auditFunction.apply(
                auditDetails,
                requestStartTime,
                requestEndTime,
                response.status,
                response.body,
                Some(result)
              )
              Future.successful(result)
            }
            .recoverTotal { (error: JsError) =>
              Future.failed(JsResult.Exception(error))
            }

      }
    }.recoverWith {

      case e: NotFoundException   =>
        auditFunction.apply(auditDetails, requestStartTime, Instant.now, e.responseCode, e.message, None)

        Future.failed(UpstreamErrorResponse(e.message, NOT_FOUND))

      case e: Upstream5xxResponse =>
        auditFunction.apply(auditDetails, requestStartTime, Instant.now, e.statusCode, e.message, None)

        Future.failed(UpstreamErrorResponse(e.message, INTERNAL_SERVER_ERROR))

      case e: UpstreamErrorResponse =>
        auditFunction.apply(auditDetails, requestStartTime, Instant.now, e.statusCode, e.message, None)
        Future.failed(e)

      case e: Exception =>
        //E.g. Any error not directly related to the http call, e.g. Json parsing
        auditFunction.apply(auditDetails, requestStartTime, Instant.now, OK, e.getMessage, None)
        Future.failed(e)

    }
  }

  def getCommodityCode(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    journey: String,
    recordId: Option[String]
  )(implicit hc: HeaderCarrier): Future[Commodity] =
    getFromOtt[Commodity](
      commodityCode,
      ottCommoditiesUrl,
      "bearerToken",
      OttAuditData(eori, affinityGroup, recordId, commodityCode, None, None, Some(journey)),
      auditService.auditValidateCommodityCode
    )

  def getCategorisationInfo(
    commodityCode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: Option[String],
    countryOfOrigin: String,
    dateOfTrade: LocalDate
  )(implicit hc: HeaderCarrier): Future[OttResponse] =
    getFromOtt[OttResponse](
      commodityCode,
      ottGreenLanesUrl,
      "bearerToken",
      OttAuditData(eori, affinityGroup, recordId, commodityCode, Some(countryOfOrigin), Some(dateOfTrade), None),
      auditService.auditGetCategorisationAssessmentDetails
    )

}
