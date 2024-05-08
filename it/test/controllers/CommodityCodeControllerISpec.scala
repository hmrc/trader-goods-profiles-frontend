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
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class CommodityCodeControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]
  private val url           = s"http://localhost:$port${routes.CommodityCodeController.onPageLoad.url}"

  "commodity code controller" should {

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

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("What is the commodity code for your goods?"))

    }

    //TODO: Should change Dummy controller to actual when it becomes available
    "redirect to dummy controller when submitting valid data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("commodityCode" -> "654321")))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)

    }

    "return bad request when submitting invalid format" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("commodityCode" -> "ABCDEF")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Enter a commodity code in the correct format"))

    }

    "return bad request when submitting incorrect data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("commodityCode" -> "10987654321")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Enter a commodity code in the correct format"))

    }

    "return bad request when submitting no data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("commodityCode" -> "")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Enter a commodity code"))

    }
  }

}
