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

import helpers.ItTestBase
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class UkimsNumberControllerISpec extends ItTestBase {
  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private val url = s"http://localhost:$port${routes.UkimsNumberController.onPageLoad.url}"

  "Ukims number controller" should {

    "redirects you to unauthorised page when auth fails" in {

      noEnrolment

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

    }

    "loads page" in {

      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

    }

    "redirects to dummy controller when submitting valid data" in {

      authorisedUser

      val validUkimsNumber = "XI47699357400020231115081800"

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("ukimsNumber" -> validUkimsNumber)))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)

    }

    "returns bad request when submitting no data" in {

      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe BAD_REQUEST

    }

    "returns bad request when submitting invalid data" in {

      authorisedUser

      val invalidUkimsNumber = "XI4769935740002023111508"

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("ukimsNumber" -> invalidUkimsNumber)))

      response.status mustBe BAD_REQUEST

    }
  }
}
