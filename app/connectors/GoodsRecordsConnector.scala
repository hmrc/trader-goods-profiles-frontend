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
import models.responses.GoodsRecordResponse
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.JsResult
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordsConnector @Inject()(config: Configuration, httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext){

  private val routerBaseUrl: Service      = config.get[Service]("microservice.services.trader-goods-profiles-router")

  private def getRecordUrl(eori: String, recordId: String) =
    url"$routerBaseUrl/trader-goods-profiles-router/$eori/records/$recordId"

  def getRecord(eori: String, recordId: String)(implicit hc: HeaderCarrier): Future[GoodsRecordResponse] = {
    httpClient
      .get(getRecordUrl(eori, recordId))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            response.json
              .validate[GoodsRecordResponse]
              .map(result => Future.successful(result))
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
        }
      }

  }

}
