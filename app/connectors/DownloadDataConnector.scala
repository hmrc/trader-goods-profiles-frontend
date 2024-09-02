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
import models.{DownloadDataSummary, Email}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
  private val dataStoreBaseUrl: Service = config.get[Service]("microservice.services.trader-goods-profiles-data-store")
  private val clientIdHeader            = ("X-Client-ID", "tgp-frontend")

  private def downloadDataSummaryUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/download-data-summary"

  private def emailUrl(eori: String) =
    url"$dataStoreBaseUrl/trader-goods-profiles-data-store/traders/$eori/email"

  def requestDownloadData(eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(downloadDataSummaryUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED => Done
        }
      }

  def getDownloadDataSummary(eori: String)(implicit hc: HeaderCarrier): Future[Option[DownloadDataSummary]] =
    httpClient
      .get(downloadDataSummaryUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(response.json.as[DownloadDataSummary])
        }
      }
      .recover { case _: NotFoundException =>
        None
      }

  def getEmail(eori: String)(implicit hc: HeaderCarrier): Future[Option[Email]] =
    httpClient
      .get(emailUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(response.json.as[Email])
        }
      }
      .recover { case _: NotFoundException =>
        None
      }
}
