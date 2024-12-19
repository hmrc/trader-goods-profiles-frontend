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

import config.FrontendAppConfig
import models.{DownloadData, DownloadDataSummary, Email, LegacyRawReads}
import org.apache.pekko.Done
import play.api.http.Status.{ACCEPTED, NOT_FOUND, NO_CONTENT, OK}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataConnector @Inject() (config: FrontendAppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends LegacyRawReads {

  private val clientIdHeader = ("X-Client-ID", "tgp-frontend")

  private def downloadDataSummaryUrl =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/download-data-summary"

  private def downloadDataUrl =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/download-data"

  private def emailUrl =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/email"

  def requestDownloadData(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(downloadDataUrl)
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case ACCEPTED => Future.successful(Done)
          case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def getDownloadDataSummary(implicit hc: HeaderCarrier): Future[Seq[DownloadDataSummary]] =
    if (config.downloadFileEnabled) {
      httpClient
        .get(downloadDataSummaryUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK => response.json.as[Seq[DownloadDataSummary]]
            case _  => Seq.empty
          }
        }
    } else {
      Future.successful(Seq.empty)
    }

  def getDownloadData(implicit hc: HeaderCarrier): Future[Seq[DownloadData]] =
    if (config.downloadFileEnabled) {
      httpClient
        .get(downloadDataUrl)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK => response.json.as[Seq[DownloadData]]
            case _  => Seq.empty
          }
        }
    } else {
      Future.successful(Seq.empty)
    }

  def getEmail(implicit hc: HeaderCarrier): Future[Option[Email]] =
    httpClient
      .get(emailUrl)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(Some(response.json.as[Email]))
          case NOT_FOUND => Future.successful(None)
          case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
      .recover {
        case x: UpstreamErrorResponse if x.statusCode == NOT_FOUND =>
          None
      }

  def updateSeenStatus(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .patch(downloadDataSummaryUrl)
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.successful(Done)
          case _          => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
}
