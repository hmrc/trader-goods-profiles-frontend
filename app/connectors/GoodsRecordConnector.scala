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

import cats.data.EitherT
import config.Service
import models.{CategoryRecord, GoodsRecord, RecordsSummary, UpdateGoodsRecord}
import models.router.requests.{CreateRecordRequest, UpdateRecordRequest}
import models.router.responses.{GetGoodsRecordResponse, GetRecordsResponse}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{ACCEPTED, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
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

  private def checkGoodsRecordsUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/checkRecords"

  private def recordsSummaryUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records-summary"

  private def getGoodsRecordCountsUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/count"

  private def storeAllGoodsRecordsUrl(
    eori: String
  ) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/store"

  private def filterRecordsUrl(eori: String, queryParams: Map[String, String]) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/filter?$queryParams"

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
      .map { response =>
        response.status match {
          case NO_CONTENT => true
        }
      }
      .recover { case e: NotFoundException =>
        false
      }

  def updateGoodsRecord(updateGoodsRecord: UpdateGoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(goodsRecordUrl(updateGoodsRecord.eori, updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(UpdateRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def updateCategoryForGoodsRecord(eori: String, recordId: String, categoryRecord: CategoryRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(goodsRecordUrl(eori, recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(UpdateRecordRequest.mapFromCategory(categoryRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

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
  ): Future[Either[Done, GetRecordsResponse]] = {

    val queryParams = Map(
      "page" -> page.toString,
      "size" -> size.toString
    )

    httpClient
      .get(goodsRecordsUrl(eori, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK       => Right(response.json.as[GetRecordsResponse])
          case ACCEPTED => Left(Done)
        }
      }
  }

  def getRecordsCount(
    eori: String
  )(implicit
    hc: HeaderCarrier
  ): Future[Int] =
    httpClient
      .get(getGoodsRecordCountsUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[Int])

  def getRecordsSummary(
    eori: String
  )(implicit hc: HeaderCarrier): Future[RecordsSummary] =
    httpClient
      .head(recordsSummaryUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[RecordsSummary])

  def filterRecordsByField(
    eori: String,
    searchTerm: String,
    field: String
  )(implicit
    hc: HeaderCarrier
  ): Future[GetRecordsResponse] = {

    val queryParams = Map(
      "searchTerm" -> searchTerm,
      "field"      -> field
    )

    httpClient
      .get(filterRecordsUrl(eori, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetRecordsResponse])
  }

  def searchRecords(
    eori: String,
    searchTerm: String,
    exactMatch: Boolean,
    page: Int,
    size: Int
  )(implicit
    hc: HeaderCarrier
  ): Future[GetRecordsResponse] = {

    val queryParams = Map(
      "page" -> page.toString,
      "size" -> size.toString
    )

    httpClient
      .get(searchRecordsUrl(eori, searchTerm, exactMatch, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetRecordsResponse])
  }
}
