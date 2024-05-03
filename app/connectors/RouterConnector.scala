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
import models.Eori
import models.router.responses.SetUpProfileResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject()(
                       appConfig: FrontendAppConfig,
                       httpClientV2: HttpClientV2
                     ) extends BaseConnector {

  def setUpProfile(eori: Eori)(implicit ec: ExecutionContext, hc: HeaderCarrier) = {

    val routerService = appConfig.tgpRouter
    val url = s"${routerService.baseUrl}/customs/traders/good-profiles"

    // Create request to send - jsony stuff
    // TODO pass in user answers
    // TODO Make a pretty JSON

    // Do the request - httpy stuff

    // Get the response - jsony stuff

    httpClientV2.put(url"$url/${eori.value}")
      .execute[HttpResponse]
     // .executeAndDeserialise[SetUpProfileResponse]


    // TODO unit tests
    // success
    // error handling

    // TODO IT?
    // Update them to mock the connector??
    // Wiremock?

  }
}
