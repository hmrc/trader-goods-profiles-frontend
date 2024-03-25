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
import models.ott.OttResponse
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Results.{BadRequest, InternalServerError, Status}
import play.api.mvc.{Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, StringContextOps}

@Singleton
class OttConnector @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  private def setHeaders(): (String, String) = (
    "Authorization" -> "Token ???"
  )

  def getGoodsNomenclatures(comcode: String)(implicit hc: HeaderCarrier): Future[OttResponse] = {
    val responseFuture = requestDataFromOtt(comcode)
    responseFuture.flatMap { httpResponse =>
      httpResponse.status match {
        case OK =>
          val json = Json.parse(httpResponse.body)
          json.validate[OttResponse] match {
            case JsSuccess(ottResponse, _) =>
              Future.successful(ottResponse)
            case JsError(errors) =>
              Future.failed(new Exception("Failed to parse OTT response: " + errors.mkString(", ")))
          }
        case _ =>
          Future.failed(new Exception(s"Failure status from OTT. Code: ${httpResponse.status.toString}  Body: ${httpResponse.body}"))
      }
    }.recover {
      case exception: Exception =>
        throw new Exception("Error communicating with OTT: " + exception.getMessage)
    }
  }

  def requestDataFromOtt(comcode: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlString = s"${appConfig.ottBaseUrl}${appConfig.ottGreenLanePath}${comcode}"
    val url = url"${urlString}"
    val responseFuture = http
      .get(url)
      .addHeaders(setHeaders())
      .withProxy
      .execute[HttpResponse]
    responseFuture
  }
}