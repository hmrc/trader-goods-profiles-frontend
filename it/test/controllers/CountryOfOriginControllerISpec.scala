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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class CountryOfOriginControllerISpec extends ItTestBase {

  private val fieldName = "countryOfOrigin"

  "Country of origin controller" should {

    "redirect to unauthorised page when authorisation fails" in {
      noEnrolment

      val result = callRoute(FakeRequest(routes.CountryOfOriginController.onPageLoad))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "ok on loading page" in {
      authorisedUserWithAnswers

      val result = callRoute(FakeRequest(routes.CountryOfOriginController.onPageLoad))

      status(result) mustBe OK
      html(result) must include("country")
    }

    "redirect to dummy controller when submitting valid data" in {
      authorisedUserWithAnswers

      val validCountryCode = "GB"
      val result           = callRoute(
        FakeRequest(routes.CountryOfOriginController.onSubmit(saveAndReturn = false))
          .withFormUrlEncodedBody(fieldName -> validCountryCode)
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.DummyController.onPageLoad.url)
    }

    "bad request when submitting no data" in {
      authorisedUserWithAnswers

      val result =
        callRoute(
          FakeRequest(routes.CountryOfOriginController.onSubmit(saveAndReturn = false))
            .withFormUrlEncodedBody(fieldName -> "")
        )

      status(result) mustBe BAD_REQUEST
      html(result) must include("error")
    }

    "bad request when submitting invalid data" in {
      authorisedUserWithAnswers

      val invalidCountryCode = "3S2@"
      val result             = callRoute(
        FakeRequest(routes.CountryOfOriginController.onSubmit(saveAndReturn = false))
          .withFormUrlEncodedBody(fieldName -> invalidCountryCode)
      )

      status(result) mustBe BAD_REQUEST
      html(result) must include("error")
    }
  }
}
