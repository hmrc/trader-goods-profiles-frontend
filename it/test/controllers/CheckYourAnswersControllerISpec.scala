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

package controllers

import base.{ItTestBase, WireMockServerSpec}
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, put, stubFor, urlEqualTo}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.test.WireMockSupport

class CheckYourAnswersControllerISpec extends ItTestBase with WireMockServerSpec {
  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private val url = s"http://localhost:$port${routes.CheckYourAnswersController.onPageLoad.url}"

  lazy private val baseWireMockUrl = s"http://$wireMockHost:$wireMockPort"

  override def appBuilder: GuiceApplicationBuilder = {

    wireMock.start()
    WireMock.configureFor(wireMockHost, wireMockPort)

    super.appBuilder.configure(
      //  WireMock.configureFor(wireHost, wireMock.port())

      "microservice.services.trader-goods-profile-router.host" -> wireMockHost,
      "microservice.services.trader-goods-profile-router.port" -> wireMockPort,
    )
  }

  "CheckYourAnswersController" should {

    "redirect you to unauthorised page when auth fails" in {

      noEnrolment

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

    }

    "loads page" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

    }

    "redirect to homepage controller when submitting valid data is successful" in {

      stubFor(
        put(urlEqualTo(s"/customs/traders/good-profiles/$generatedEori"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.HomepageController.onPageLoad.url)

    }

    "redirect to dummy controller when submitting data is unsuccessful" in {

      stubFor(
        put(urlEqualTo(s"/customs/traders/good-profiles/$generatedEori"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("Bad times")
          )
      )

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)

    }


  }
}
