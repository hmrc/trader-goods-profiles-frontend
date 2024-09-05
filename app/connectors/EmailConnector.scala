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
import models.email.{DownloadRecordEmailParameters, DownloadRecordEmailRequest}
import models.{HistoricProfileData, TraderProfile}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{ACCEPTED, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val emailServiceBaseUrl: Service = config.get[Service]("microservice.services.email")

  private def sendEmailUrl = url"$emailServiceBaseUrl/hmrc/email"

  def sendDownloadRecordEmail(to: String, downloadRecordEmailParameters: DownloadRecordEmailParameters)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .post(sendEmailUrl)
      .withBody(Json.toJson(DownloadRecordEmailRequest(Seq(to), downloadRecordEmailParameters)))
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case ACCEPTED => Done
        }
      )
}
