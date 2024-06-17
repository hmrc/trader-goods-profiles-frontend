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
import models.Commodity
import models.ott.response.OttResponse
import play.api.Configuration
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsResult, Reads}
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
    commodityCode: String, urlFunc: String => URL, authToken: String,mode: String,
    eori: String,
    affinityGroup: AffinityGroup,
    journey: String,
    recordId: Option[String],
    countryOfOrigin: String,
    dateOfTrade: LocalDate
  )(implicit
    hc: HeaderCarrier,
    reads: Reads[T]
  ): Future[T] = {
    val newHeaderCarrier = hc.copy(authorization = Some(Authorization(authToken)))

    val requestStartTime = Instant.now
    httpClient
      .get(urlFunc(commodityCode))(newHeaderCarrier)
      .execute[HttpResponse]
      .flatMap { response =>

        val requestEndTime = Instant.now

        response.status match {
          case OK =>

            response.json
              .validate[T]
              .map(result => {
                if (mode == "commodity") {
                  auditService.auditValidateCommodityCode(
                    eori,
                    affinityGroup,
                    journey,
                    recordId,
                    commodityCode,
                    requestStartTime,
                    requestEndTime,
                    true,
                    codeDescriptions(response.status),
                    response.status,
                    "null",
                    result.asInstanceOf[Commodity].description,
                    result.asInstanceOf[Commodity].validityEndDate,
                    result.asInstanceOf[Commodity].validityStartDate
                  )
                } else {
                  auditService.auditGetCategorisationAssessmentDetails(
                    eori,
                    affinityGroup,
                    recordId,
                    commodityCode,
                    countryOfOrigin,
                    dateOfTrade,
                    requestStartTime,
                    requestEndTime,
                    codeDescriptions(response.status),
                    response.status,
                    "null",
                    result.asInstanceOf[OttResponse].categoryAssessments.size,
                    result.asInstanceOf[OttResponse].categoryAssessments.map(x => x.exemptions.size).sum
                  )
                }

                Future.successful(result)
              })
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
        }
      }
      .recoverWith {

        case e: NotFoundException   =>

          if (mode == "commodity") {
            auditService.auditValidateCommodityCode(
              eori,
              affinityGroup,
              journey,
              recordId,
              commodityCode,
              requestStartTime,
              Instant.now,
              false,
              codeDescriptions(e.responseCode),
              e.responseCode,
              e.message,
              "null",
              None,
              Instant.now
            )
          } else {
            auditService.auditGetCategorisationAssessmentDetails(
              eori,
              affinityGroup,
              recordId,
              commodityCode,
              countryOfOrigin,
              dateOfTrade,
              requestStartTime,
              Instant.now,
              codeDescriptions(e.responseCode),
              e.responseCode,
              e.message,
              0,
              0
            )
          }
          Future.failed(UpstreamErrorResponse(e.message, NOT_FOUND))
        case e: Upstream5xxResponse =>
          if (mode == "commodity") {
            auditService.auditValidateCommodityCode(
              eori,
              affinityGroup,
              journey,
              recordId,
              commodityCode,
              requestStartTime,
              Instant.now,
              false,
              codeDescriptions(e.statusCode),
              e.statusCode,
              e.message,
              "null",
              None,
              Instant.now
            )
          } else {
            auditService.auditGetCategorisationAssessmentDetails(
              eori,
              affinityGroup,
              recordId,
              commodityCode,
              countryOfOrigin,
              dateOfTrade,
              requestStartTime,
              Instant.now,
              codeDescriptions(e.statusCode),
              e.statusCode,
              e.message,
              0,
              0
            )
          }

          Future.failed(UpstreamErrorResponse(e.message, INTERNAL_SERVER_ERROR))

        case f: UpstreamErrorResponse =>
          if (mode == "commodity") {
            auditService.auditValidateCommodityCode(
              eori, //bef
              affinityGroup, //bef
              journey, //bef
              recordId, //bef
              commodityCode, //bef
              requestStartTime, //loc
              Instant.now, //loc
              false, //resp
              codeDescriptions(f.statusCode), //resp
              f.statusCode, //resp
              f.message, //resp
              "null", //res
              None, //res
              Instant.now //res
            )
          } else {
            auditService.auditGetCategorisationAssessmentDetails(
              eori, //bef
              affinityGroup, //bef
              recordId, //bef
              commodityCode, //bef
              countryOfOrigin,//bef
              LocalDate.now(),//bef
              requestStartTime, //local
              Instant.now, //local
              codeDescriptions(f.statusCode), //response
              f.statusCode, //response
              f.message, // response
              0, //result
              0 //result
            )
          }

          Future.failed(f)


      }
  }

  def getCommodityCode(commodityCode: String, eori: String, affinityGroup: AffinityGroup, journey: String,
                       recordId: Option[String])(implicit hc: HeaderCarrier): Future[Commodity] =
    getFromOtt[Commodity](commodityCode, ottCommoditiesUrl, "bearerToken", "commodity", eori, affinityGroup, journey
    , recordId, "N/A", LocalDate.now())

  def getCategorisationInfo(commodityCode: String, eori: String, affinityGroup: AffinityGroup
                           ,recordId: Option[String], countryOfOrigin: String, dateOfTrade: LocalDate)(implicit hc: HeaderCarrier): Future[OttResponse] =
    getFromOtt[OttResponse](commodityCode, ottGreenLanesUrl, "bearerToken", "cat", eori,
      affinityGroup, "N/A", recordId, countryOfOrigin, dateOfTrade)

}
