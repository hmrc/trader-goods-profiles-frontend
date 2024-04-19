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
import play.api.http.Status.OK

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import com.github.jasminb.jsonapi.ResourceConverter
import models.ott.GoodsNomenclature
import models.ott.util.OttJsonApiParser

@Singleton
class OttConnector @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  val rc: ResourceConverter = new ResourceConverter(classOf[GoodsNomenclature])

  private def setHeaders(): (String, String) = (
    "Authorization" -> "Token ???"
  )

  def getGoodsNomenclatures(comcode: String)(implicit hc: HeaderCarrier): Future[GoodsNomenclature] = {
    requestDataFromOtt(comcode).map { res =>
      res.status match {
        case OK => OttJsonApiParser.parse(res.body)
        case _ => throw new Exception("OTT responded with status " + res.status.toString)
      }
    }.recover {
      case exception: Exception =>
        throw new Exception("Error communicating with OTT: " + exception.getMessage)
    }
  }
//
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