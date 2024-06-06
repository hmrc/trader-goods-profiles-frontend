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
import models.GoodsRecord
import models.router.requests.CreateRecordRequest
import models.router.responses.{CreateGoodsRecordResponse, GetGoodsRecordResponse}
import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.{JsResult, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
  private val tgpRouterBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val goodsRecordUrl            = url"$tgpRouterBaseUrl/trader-goods-profiles-router/records"
  private val clientIdHeader            = ("X-Client-ID", "tgp-frontend")

  private def getRecordUrl(eori: String, recordId: String) =
    url"$tgpRouterBaseUrl/trader-goods-profiles-router/$eori/records/$recordId"

  def submitGoodsRecord(goodsRecord: GoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[CreateGoodsRecordResponse] =
    httpClient
      .post(goodsRecordUrl)
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(CreateRecordRequest.map(goodsRecord)))
      .execute[HttpResponse]
      .map(response => response.json.as[CreateGoodsRecordResponse])

  def getRecord(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetGoodsRecordResponse] =
    httpClient
      .get(getRecordUrl(eori, recordId))
      .addHeaders(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            response.json
              .validate[GetGoodsRecordResponse]
              .map(result => Future.successful(result))
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
        }
      }
}
