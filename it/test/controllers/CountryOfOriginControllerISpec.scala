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

import base.ItTestBase
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers._

class CountryOfOriginControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]
  private val url           = s"http://localhost:$port${routes.CountryOfOriginController.onPageLoad.url}"
  private val submitUrl     =
    s"http://localhost:$port${routes.CountryOfOriginController.onSubmit(saveAndReturn = false).url}"

  private val fieldName = "countryOfOrigin"

  "Country of origin controller" should {

    "redirect to unauthorised page when authorisation fails" in {
      noEnrolment

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "ok on loading page" in {
      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Country of origin"))
    }

    "redirect to dummy controller when submitting valid data" in {
      authorisedUserWithAnswers

      val request: WSRequest = client.url(submitUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "GB")))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)

    }

    "bad request when submitting no data" in {
      authorisedUserWithAnswers

      val request: WSRequest = client.url(submitUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("error"))

    }

    "bad request when submitting invalid data" in {
      authorisedUserWithAnswers

      val request: WSRequest = client.url(submitUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "3S2@")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("error"))
    }
  }
}
