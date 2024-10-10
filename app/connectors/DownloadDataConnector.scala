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
import models.{DownloadData, DownloadDataSummary, Email}
import org.apache.pekko.Done
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataConnector @Inject() (config: FrontendAppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {
  private val clientIdHeader = ("X-Client-ID", "tgp-frontend")

  private def downloadDataSummaryUrl(eori: String) =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/$eori/download-data-summary"

  private def downloadDataUrl(eori: String) =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/$eori/download-data"

  private def emailUrl(eori: String) =
    url"${config.dataStoreBaseUrl}/trader-goods-profiles-data-store/traders/$eori/email"

  def requestDownloadData(eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(downloadDataUrl(eori))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case ACCEPTED => Future.successful(Done)
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def getDownloadDataSummary(eori: String)(implicit hc: HeaderCarrier): Future[Option[DownloadDataSummary]] =
    if (config.downloadFileEnabled) {
      httpClient
        .get(downloadDataSummaryUrl(eori))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case OK => Some(response.json.as[DownloadDataSummary])
            case _ => None
          }
        }
        .recover { case _: NotFoundException =>
          None
        }
    } else {
      Future.successful(None)
    }

  def getDownloadData(eori: String)(implicit hc: HeaderCarrier): Future[Option[DownloadData]] =
    httpClient
      .get(downloadDataUrl(eori))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(response.json.as[DownloadData])
          case _  => None
        }
      }
      .recover { case _: NotFoundException =>
        None
      }

  def getEmail(eori: String)(implicit hc: HeaderCarrier): Future[Email] =
    httpClient
      .get(emailUrl(eori))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          //TODO this also retunrs a 404 but we are choosing to ignore it because at some point we are going to put in a check when anyone enters our service to check that they have a verified and deliverable email so this should never be 404
          case OK => Future.successful(response.json.as[Email])
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
}
