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

import config.{FrontendAppConfig, Service}
import models.router.requests.{CreateRecordRequest, PatchRecordRequest, PutRecordRequest}
import models.router.responses.{GetGoodsRecordResponse, GetRecordsResponse}
import models.{CategoryRecord, GoodsRecord, LegacyRawReads, RecordsSummary, SupplementaryRequest, UpdateGoodsRecord}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{ACCEPTED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends LegacyRawReads {
  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val clientIdHeader            = ("X-Client-ID", "tgp-frontend")

  private def createGoodsRecordUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records"

  private def deleteGoodsRecordUrl(eori: String, recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/$recordId"

  private def singleGoodsRecordUrl(eori: String, recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/$recordId"

  private def goodsRecordUrl(eori: String, recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/$recordId"

  private def goodsRecordsUrl(eori: String, queryParams: Map[String, String]) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records?$queryParams"

  private def recordsSummaryUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records-summary"

  private def filterRecordsUrl(eori: String, queryParams: Map[String, String]) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/filter?$queryParams"

  private def isTraderReferenceUniqueUrl(traderReference: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/is-trader-reference-unique/$traderReference"

  private def searchRecordsUrl(
    eori: String,
    searchTerm: String,
    exactMatch: Boolean,
    queryParams: Map[String, String]
  ) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/filter?searchTerm=$searchTerm&exactMatch=$exactMatch&$queryParams"

  def submitGoodsRecord(goodsRecord: GoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    httpClient
      .post(createGoodsRecordUrl(goodsRecord.eori))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(CreateRecordRequest.map(goodsRecord)))
      .execute[HttpResponse]
      .map(response => response.body)

  def removeGoodsRecord(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Boolean] =
    httpClient
      .delete(deleteGoodsRecordUrl(eori, recordId))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.successful(true)
          case NOT_FOUND  => Future.successful(false)
          case _          => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
      .recover {
        case x: UpstreamErrorResponse if x.statusCode == NOT_FOUND =>
          false
      }

  // TODO: remove this function when EIS has implemented the PATCH method - TGP-2417 and keep putGoodsRecord and patchGoodsRecord
  def updateGoodsRecord(updateGoodsRecord: UpdateGoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(goodsRecordUrl(updateGoodsRecord.eori, updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(PatchRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def patchGoodsRecord(updateGoodsRecord: UpdateGoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(goodsRecordUrl(updateGoodsRecord.eori, updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(PatchRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def putGoodsRecord(updateGoodsRecord: PutRecordRequest, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .put(goodsRecordUrl(updateGoodsRecord.actorId, recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(updateGoodsRecord))
      .execute[HttpResponse]
      .map(_ => Done)

  def updateCategoryAndComcodeForGoodsRecord(
    eori: String,
    recordId: String,
    categoryRecord: CategoryRecord,
    oldRecord: GetGoodsRecordResponse
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the put call as default
    if (appConfig.useEisPatchMethod) {
      httpClient
        .put(goodsRecordUrl(eori, recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PutRecordRequest.mapFromCategoryAndComcode(categoryRecord, oldRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    } else {
      httpClient
        .patch(goodsRecordUrl(eori, recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PatchRecordRequest.mapFromCategoryAndComcode(categoryRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    }

  def updateSupplementaryUnitForGoodsRecord(
    eori: String,
    recordId: String,
    supplementaryRequest: SupplementaryRequest,
    oldRecord: GetGoodsRecordResponse
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the put call as default
    if (appConfig.useEisPatchMethod) {
      httpClient
        .put(goodsRecordUrl(eori, recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PutRecordRequest.mapFromSupplementary(supplementaryRequest, oldRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    } else {
      httpClient
        .patch(goodsRecordUrl(eori, recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PatchRecordRequest.mapFromSupplementary(supplementaryRequest)))
        .execute[HttpResponse]
        .map(_ => Done)
    }

  def getRecord(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetGoodsRecordResponse] =
    httpClient
      .get(singleGoodsRecordUrl(eori, recordId))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetGoodsRecordResponse])

  def getRecords(
    eori: String,
    page: Int,
    size: Int
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[GetRecordsResponse]] = {

    val queryParams = Map(
      "page" -> page.toString,
      "size" -> size.toString
    )

    httpClient
      .get(goodsRecordsUrl(eori, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK       => Future.successful(Some(response.json.as[GetRecordsResponse]))
          case ACCEPTED => Future.successful(None)
          case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
  }

  def getRecordsSummary(
    eori: String
  )(implicit hc: HeaderCarrier): Future[RecordsSummary] =
    httpClient
      .get(recordsSummaryUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.json.as[RecordsSummary]
      }

  def isTraderReferenceUnique(traderReference: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .get(isTraderReferenceUniqueUrl(traderReference))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        (response.json \ "isUnique").as[Boolean]
      }

  def filterRecordsByField(
    eori: String,
    searchTerm: String,
    field: String
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[GetRecordsResponse]] = {

    val queryParams = Map(
      "searchTerm" -> searchTerm,
      "field"      -> field
    )

    httpClient
      .get(filterRecordsUrl(eori, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK       => Future.successful(Some(response.json.as[GetRecordsResponse]))
          case ACCEPTED => Future.successful(None)
          case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
  }

  def searchRecords( // TODO: add more parameter when we pick TGP:3003 and change the condition in the else.
    eori: String,
    searchTerm: String,
    exactMatch: Boolean,
    page: Int,
    size: Int
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[GetRecordsResponse]] = {

    val queryParams = Map(
      "page" -> page.toString,
      "size" -> size.toString
    )
    if (appConfig.enhancedSearch) { // TODO: remove this flag when
      httpClient
        .get(searchRecordsUrl(eori, searchTerm, exactMatch, queryParams))
        .setHeader(clientIdHeader)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK       => Future.successful(Some(response.json.as[GetRecordsResponse]))
            case ACCEPTED => Future.successful(None)
            case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
          }
        }
    } else {
      httpClient
        .get(searchRecordsUrl(eori, searchTerm, exactMatch, queryParams))
        .setHeader(clientIdHeader)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK       => Future.successful(Some(response.json.as[GetRecordsResponse]))
            case ACCEPTED => Future.successful(None)
            case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
          }
        }
    }
  }
}
