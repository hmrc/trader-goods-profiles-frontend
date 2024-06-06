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
import models.router.CreateRecordRequest
import models.{CreateGoodsRecord, CreateGoodsRecordResponse}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
  private val xClientId: String         = config.get[String]("headers.xClientId")
  private val tgpRouterBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val goodsRecordUrl            = url"$tgpRouterBaseUrl/trader-goods-profiles-router/records"

  private def mapToCreateRecordRequest(goodsRecord: CreateGoodsRecord): CreateRecordRequest =
    CreateRecordRequest(
      goodsRecord.eori,
      goodsRecord.eori,
      goodsRecord.traderRef,
      goodsRecord.comcode,
      goodsRecord.goodsDescription,
      goodsRecord.countryOfOrigin,
      goodsRecord.comcodeEffectiveFromDate,
      goodsRecord.comcodeEffectiveToDate
    )

  def submitGoodsRecord(goodsRecord: CreateGoodsRecord)(implicit
    hc: HeaderCarrier
  ): Future[CreateGoodsRecordResponse] =
    httpClient
      .post(goodsRecordUrl)
      .setHeader(header = ("X-Client-ID", xClientId))
      .withBody(Json.toJson(mapToCreateRecordRequest(goodsRecord)))
      .execute[HttpResponse]
      .map(response => response.json.as[CreateGoodsRecordResponse])
}
