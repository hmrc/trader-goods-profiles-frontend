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
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}

class NirmsNumberControllerISpec  extends ItTestBase {

  "Nirms number controller" should {

    "redirect you to unauthorised page when auth fails" in {

      noEnrolment

      val result = callRoute(FakeRequest(routes.NirmsNumberController.onPageLoad))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

    }

    "loads page" in {

      authorisedUser

      val result = callRoute(FakeRequest(routes.NirmsNumberController.onPageLoad))

      status(result) mustBe OK

      html(result) must include ("What is your NIRMS number?")

    }


    //TODO: Should change Dummy controller to actual when it becomes available
    "redirect to dummy controller when submitting valid data" in {

      authorisedUser

      val result = callRoute(FakeRequest(routes.NirmsNumberController.onSubmit).withFormUrlEncodedBody("nirmsNumber" -> "RMS-GB-123456"))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.DummyController.onPageLoad.url)

    }

    "return bad request when submitting invalid data" in {

      authorisedUser

      val result = callRoute(FakeRequest(routes.NirmsNumberController.onSubmit).withFormUrlEncodedBody("nirmsNumber" -> "ABC-GF-123456"))

      status(result) mustBe BAD_REQUEST

      html(result) must include ("Enter your NIRMS number in the correct format")

    }

    "return bad request when submitting incorrect format" in {

      authorisedUser
      val result = callRoute(FakeRequest(routes.NirmsNumberController.onSubmit).withFormUrlEncodedBody("nirmsNumber" -> "AB2343534"))

      status(result) mustBe BAD_REQUEST

      html(result) must include ("Enter your NIRMS number in the correct format")

    }

    "return bad request when submitting no data" in {

      authorisedUser
      val result = callRoute(FakeRequest(routes.NirmsNumberController.onSubmit).withFormUrlEncodedBody("nirmsNumber" -> ""))

      status(result) mustBe BAD_REQUEST

      html(result) must include ("Enter your NIRMS number")

    }
  }

}
