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
import models.{CategorisationAnswers, CommodityCode, CountryOfOrigin, MaintainProfileAnswers, NiphlNumber, NirmsNumber, UkimsNumber, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.SessionRepository

import scala.concurrent.Future

class CheckYourAnswersControllerISpec extends ItTestBase {
  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private lazy val repo = app.injector.instanceOf[SessionRepository]

  private val url = s"http://localhost:$port${routes.CheckYourAnswersController.onPageLoad.url}"

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

      val categorisationAnswers =
        CategorisationAnswers(Some(CommodityCode("anything")), Some(CountryOfOrigin("GB")))

      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(true),
          Some(NirmsNumber("anything")),
          Some(true),
          Some(NiphlNumber("anything"))
        )

      val fullUserAnswers = UserAnswers("internalId", maintainProfileAnswers, categorisationAnswers)

      await(repo.set(fullUserAnswers))

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

    }

    "redirect to dummy controller when submitting valid data" in {

      authorisedUserWithAnswers

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)

    }

  }
}
