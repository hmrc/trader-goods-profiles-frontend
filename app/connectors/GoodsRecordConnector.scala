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
import models.{CategoryRecord, GoodsRecord, UpdateGoodsRecord}
import models.router.requests.{CreateRecordRequest, UpdateCategoryRecordRequest, UpdateRecordRequest}
import models.router.responses.{CreateGoodsRecordResponse, GetGoodsRecordResponse, GetRecordsResponse}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
  private val tgpRouterBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val clientIdHeader            = ("X-Client-ID", "tgp-frontend")

  private def createGoodsRecordUrl(eori: String) =
    url"$tgpRouterBaseUrl/trader-goods-profiles-router/traders/$eori/records"

  private def deleteGoodsRecordUrl(eori: String, recordId: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records/$recordId"

  private def singleGoodsRecordUrl(eori: String, recordId: String) =
    url"$tgpRouterBaseUrl/trader-goods-profiles-router/traders/$eori/records/$recordId"

  private def goodsRecordsUrl(eori: String, queryParams: Map[String, String]) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records?$queryParams"

  private def getGoodsRecordsUrl(
    eori: String
  ) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/records"

  def submitGoodsRecord(goodsRecord: GoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[CreateGoodsRecordResponse] =
    httpClient
      .post(createGoodsRecordUrl(goodsRecord.eori))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(CreateRecordRequest.map(goodsRecord)))
      .execute[HttpResponse]
      .map(response => response.json.as[CreateGoodsRecordResponse])

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
      .patch(singleGoodsRecordUrl(updateGoodsRecord.eori, updateGoodsRecord.recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(UpdateRecordRequest.map(updateGoodsRecord)))
      .execute[HttpResponse]
      .map(_ => Done)

  def updateCategoryForGoodsRecord(eori: String, recordId: String, categoryRecord: CategoryRecord)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .patch(singleGoodsRecordUrl(eori, recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(UpdateCategoryRecordRequest.map(categoryRecord)))
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
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[GetRecordsResponse] = {

    val pageNumber  = 1
    val pageSize    = 10
    val queryParams = Map(
      "page" -> page.getOrElse(pageNumber).toString,
      "size" -> size.getOrElse(pageSize).toString
    )

    httpClient
      .get(goodsRecordsUrl(eori, queryParams))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetRecordsResponse])
  }

  def getAllRecords(
    eori: String
  )(implicit hc: HeaderCarrier): Future[GetRecordsResponse] =
    httpClient
      .get(getGoodsRecordsUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetRecordsResponse])

  def doRecordsExist(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Option[GetRecordsResponse]] =
    httpClient
      .get(getGoodsRecordsUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Some(response.json.as[GetRecordsResponse])

        }
      }
}
