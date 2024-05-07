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

import cats.data.EitherT
import config.FrontendAppConfig
import models.router.requests.SetUpProfileRequest
import models.Eori
import models.errors.RouterError
import play.api.Logging
import play.api.http.Status.{OK, isSuccessful}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RouterConnector @Inject() (
  appConfig: FrontendAppConfig,
  httpClientV2: HttpClientV2
) extends BaseConnector
    with Logging {

  //TODO make service to call this connector!
  def setUpProfile(eori: Eori, setUpProfileRequest: SetUpProfileRequest)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, RouterError, Unit] = {

    val routerService = appConfig.tgpRouter
    val url           = s"${routerService.baseUrl}/customs/traders/good-profiles/${eori.value}"

    EitherT(httpClientV2
      .put(url"$url")
      .withBody(Json.toJson(setUpProfileRequest))
      .execute
      .flatMap(httpResponse => handleProfileSetUpResponse(eori, url, httpResponse))
      .recover { case NonFatal(exception) =>
        logger.error(
          s"[RouterConnector] - Error occurred when submitting Trader Goods Profile data for EORI ${eori.value}: ${exception.getMessage}"
        )

        Left(RouterError(s"Failed to submit Trader Goods Profile data: ${exception.getMessage}", None))
      })

    // TODO unit tests
    // success
    // error handling

    // TODO IT?
    // Update them to mock the connector??
    // Wiremock?

  }

  private def handleProfileSetUpResponse(eori: Eori, url: String, httpResponse: HttpResponse) =
    if (isSuccessful(httpResponse.status)) {
      logger.info(s"[RouterConnector] - Successfully submitted Trader Goods Profile data for EORI ${eori.value}")
      Future.successful(Right())
    } else {
      //TODO check this log message is sensible
      logger.warn(
        s"[RouterConnector] - Error occurred when submitting Trader Goods Profile data for EORI ${eori.value}: ${httpResponse.body}"
      )
      Future.successful(
        Left(
          RouterError(
            upstreamResponseMessage("PUT", url, httpResponse.status, httpResponse.body),
            Some(httpResponse.status)
          )
        )
      )
    }
}
