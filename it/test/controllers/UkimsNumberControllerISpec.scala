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
import models.NormalMode
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class UkimsNumberControllerISpec extends ItTestBase {

  private val fieldName = "ukimsNumber"

  "Ukims number controller" should {

    "redirects you to unauthorised page when auth fails" in {

      noEnrolment

      val result = callRoute(FakeRequest(routes.UkimsNumberController.onPageLoad(NormalMode)))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "loads page" in {

      authorisedUserWithAnswers

      val result = callRoute(FakeRequest(routes.UkimsNumberController.onPageLoad(NormalMode)))

      status(result) mustBe OK

      html(result) must include("What is your UKIMS number?")

    }

    "redirects to NIRMS Question controller when submitting valid data" in {

      val validUkimsNumber = "XI47699357400020231115081800"

      authorisedUserWithAnswers

      val result = callRoute(
        FakeRequest(routes.UkimsNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(fieldName -> validUkimsNumber)
      )

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.NirmsQuestionController.onPageLoad(NormalMode).url)

    }

    "returns bad request when submitting no data" in {

      authorisedUserWithAnswers

      val result =
        callRoute(
          FakeRequest(routes.UkimsNumberController.onSubmit(NormalMode)).withFormUrlEncodedBody(fieldName -> "")
        )

      status(result) mustBe BAD_REQUEST

      html(result) must include("Enter your UKIMS number")

    }

    "returns bad request when submitting invalid data" in {

      authorisedUserWithAnswers

      val invalidUkimsNumber = "XI4769935740002023111508"

      val result = callRoute(
        FakeRequest(routes.UkimsNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(fieldName -> invalidUkimsNumber)
      )

      status(result) mustBe BAD_REQUEST

      html(result) must include("Enter your UKIMS number in the correct format")

    }
  }
}
