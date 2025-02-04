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
import java.net.{URL, URLEncoder}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends LegacyRawReads {
  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val clientIdHeader            = ("X-Client-ID", "tgp-frontend")

  private def createGoodsRecordUrl =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records"

  private def deleteGoodsRecordUrl(recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/$recordId"

  private def singleGoodsRecordUrl(recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/$recordId"

  private def goodsRecordUrl(recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/$recordId"

  private def goodsRecordsQueryParamasUrl(queryParams: Map[String, String]) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records?$queryParams"

  private def recordsSummaryUrl =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records-summary"

  private def filterRecordsUrl(
    eori: String,
    queryParams: Map[String, String]
  ) = // TODO: This is part of the filtering work and will be replaced with the new filter endpoint as part of TGP-3003
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/filter?$queryParams"

  private def isproductReferenceUniqueUrl(
    productReference: String
  ) = // TODO: This is part of the filtering work and will be replaced with the new filter endpoint as part of TGP-3003
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/is-trader-reference-unique/$productReference"

  private def searchRecordsUrl(
    eori: String,
    searchTerm: Option[String],
    exactMatch: Boolean,
    queryParams: Map[String, String]
  ) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/filter?searchTerm=$searchTerm&exactMatch=$exactMatch&$queryParams"

  private def filterSearchRecordsUrl(
    searchTerm: Option[String],
    countryOfOrigin: Option[String],
    immiReady: Option[Boolean],
    notReadyForIMMI: Option[Boolean],
    actionNeeded: Option[Boolean],
    queryParams: Map[String, String]
  ): URL = {

    val queryParamsSeq = Seq(
      searchTerm.map(term => s"searchTerm=${URLEncoder.encode(term, "UTF-8")}"),
      countryOfOrigin.filter(_.nonEmpty).map(origin => s"countryOfOrigin=${URLEncoder.encode(origin, "UTF-8")}"),
      immiReady.map(ready => s"IMMIReady=$ready"),
      notReadyForIMMI.map(notReady => s"notReadyForIMMI=$notReady"),
      actionNeeded.map(needed => s"actionNeeded=$needed")
    ).flatten ++ queryParams.map { case (key, value) =>
      s"${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

    val queryString = queryParamsSeq.mkString("&")

    val urlString = s"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/records/filter?$queryString"

    new URL(urlString)

  }

  def submitGoodsRecord(goodsRecord: GoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    httpClient
      .post(createGoodsRecordUrl)
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(CreateRecordRequest.map(goodsRecord)))
      .execute[HttpResponse]
      .map(response => response.body)

  def removeGoodsRecord(recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Boolean] =
    httpClient
      .delete(deleteGoodsRecordUrl(recordId))
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
      .patch(goodsRecordUrl(updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(PatchRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def patchGoodsRecord(updateGoodsRecord: UpdateGoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(goodsRecordUrl(updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(PatchRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def putGoodsRecord(updateGoodsRecord: PutRecordRequest, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .put(goodsRecordUrl(recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(updateGoodsRecord))
      .execute[HttpResponse]
      .map(_ => Done)

  def updateCategoryAndComcodeForGoodsRecord(
    recordId: String,
    categoryRecord: CategoryRecord,
    oldRecord: GetGoodsRecordResponse
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the put call as default
    if (appConfig.useEisPatchMethod) {
      httpClient
        .put(goodsRecordUrl(recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PutRecordRequest.mapFromCategoryAndComcode(categoryRecord, oldRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    } else {
      httpClient
        .patch(goodsRecordUrl(recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PatchRecordRequest.mapFromCategoryAndComcode(categoryRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    }

  def updateSupplementaryUnitForGoodsRecord(
    recordId: String,
    supplementaryRequest: SupplementaryRequest,
    oldRecord: GetGoodsRecordResponse
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the put call as default
    if (appConfig.useEisPatchMethod) {
      httpClient
        .put(goodsRecordUrl(recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PutRecordRequest.mapFromSupplementary(supplementaryRequest, oldRecord)))
        .execute[HttpResponse]
        .map(_ => Done)
    } else {
      httpClient
        .patch(goodsRecordUrl(recordId))
        .setHeader(clientIdHeader)
        .withBody(Json.toJson(PatchRecordRequest.mapFromSupplementary(supplementaryRequest)))
        .execute[HttpResponse]
        .map(_ => Done)
    }

  def getRecord(recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetGoodsRecordResponse] =
    httpClient
      .get(singleGoodsRecordUrl(recordId))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetGoodsRecordResponse])

  def getRecords(
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
      .get(goodsRecordsQueryParamasUrl(queryParams))
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

  def getRecordsSummary(implicit hc: HeaderCarrier): Future[RecordsSummary] =
    httpClient
      .get(recordsSummaryUrl)
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.json.as[RecordsSummary]
      }

  def isproductReferenceUnique(productReference: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .get(isproductReferenceUniqueUrl(productReference))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        (response.json \ "isUnique").as[Boolean]
      }

  def filterRecordsByField( // TODO: This is part of the filtering work and will be replaced with the new filter endpoint as part of TGP-3003
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
    searchTerm: Option[String] = None,
    exactMatch: Boolean,
    countryOfOrigin: Option[String],
    IMMIReady: Option[Boolean] = None,
    notReadyForIMMI: Option[Boolean] = None,
    actionNeeded: Option[Boolean] = None,
    page: Int,
    size: Int
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[GetRecordsResponse]] =
    if (!appConfig.enhancedSearch) { // TODO: remove this flag when filter search is ready
      val queryParams = Map(
        "page" -> page.toString,
        "size" -> size.toString
      )
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
      val queryParams = Map(
        "pageOpt" -> page.toString,
        "sizeOpt" -> size.toString
      )
      httpClient
        .get(
          filterSearchRecordsUrl(
            searchTerm,
            countryOfOrigin,
            IMMIReady,
            notReadyForIMMI,
            actionNeeded,
            queryParams
          )
        )
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
