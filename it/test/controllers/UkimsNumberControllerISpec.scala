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
import models.{CheckMode, NormalMode}
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers._

class UkimsNumberControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private val normalUrl = s"http://localhost:$port${routes.UkimsNumberController.onPageLoad(NormalMode).url}"
  private val checkUrl  = s"http://localhost:$port${routes.UkimsNumberController.onPageLoad(CheckMode).url}"

  private val fieldName = "ukimsNumber"

  "UkimsNumberController" should {

    "redirects you to unauthorised page when auth fails" in {

      noEnrolment

      val request: WSRequest = client.url(normalUrl).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "loads page" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(normalUrl).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("What is your UKIMS number?"))

    }

    "redirects to NIRMS Question controller when submitting valid data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(normalUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "XI47699357400020231115081800")))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.NirmsQuestionController.onPageLoad(NormalMode).url)

    }

    "returns bad request when submitting no data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(normalUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Enter your UKIMS number"))

    }

    "returns bad request when submitting invalid data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(normalUrl).withFollowRedirects(false)

      val response = await(request.post(Map(fieldName -> "XI4769935740002023111508")))

      response.status mustBe BAD_REQUEST

      val document = Jsoup.parse(response.body)

      assert(document.text().contains("Enter your UKIMS number in the correct format"))

    }

    "CheckMode" should {

      "loads page" in {

        authorisedUserWithAnswers

        val request: WSRequest = client.url(checkUrl).withFollowRedirects(false)

        val response = await(request.get())

        response.status mustBe OK

        val document = Jsoup.parse(response.body)

        assert(document.text().contains("What is your UKIMS number?"))

      }

      "redirects to Check Your Answers controller when submitting valid data" in {

        authorisedUserWithAnswers

        val request: WSRequest = client.url(checkUrl).withFollowRedirects(false)

        val response = await(request.post(Map(fieldName -> "XI47699357400020231115081800")))

        response.status mustBe SEE_OTHER

        redirectUrl(response) mustBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }
    }
  }
}
