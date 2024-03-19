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
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

@Singleton
class OttConnector @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  private def setHeaders(): Seq[(String, String)] = Seq(
    "Authorization" -> "Token ???"
  )

  def getGoodsNomenclatures(comcode: String)(implicit hc: HeaderCarrier): Future[Either[Result, OttResponse]] = {
    val url = s"${appConfig.ottBaseUrl}${appConfig.ottGreenLanePath}${comcode}"
    val responseFuture: Future[HttpResponse] = http.GET[HttpResponse](url = url, headers = setHeaders())

    responseFuture.map { httpResponse =>
      httpResponse.status match {
        case OK =>
          val json = Json.parse(httpResponse.body)
          json.validate[OttResponse] match {
            case JsSuccess(ottResponse, _) =>
              Right(ottResponse)
            case JsError(errors) =>
              Left(BadRequest("Failed to parse response: " + errors.mkString(", ")))
          }
        case _ =>
          // Handle status codes in this match... At the moment just uses OTT status to respond.
          Left(Results.Status(httpResponse.status)(httpResponse.body))
      }
    }.recover {
      case exception: Exception =>
        // Handle exceptions like timeouts or whatever.
        Left(Results.InternalServerError("An error occurred: " + exception.getMessage))
    }
  }
}