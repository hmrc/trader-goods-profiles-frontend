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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}

class CategoryGuidanceControllerISpec extends ItTestBase {

  "Category Guidance controller" should {

    "redirects you to unauthorised page when auth fails" in {
      noEnrolment

      val result = callRoute(FakeRequest(routes.CategoryGuidanceController.onPageLoad))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "loads page" in {
      authorisedUserWithAnswers

      val result = callRoute(FakeRequest(routes.CategoryGuidanceController.onPageLoad))

      status(result) mustBe OK

      html(result) must include("Categorisation")
    }

    "returns redirect when submitting" in {
      authorisedUserWithAnswers

      val result = callRoute(FakeRequest(routes.CategoryGuidanceController.onSubmit))

      status(result) mustBe SEE_OTHER
      // TODO - Change to actual controller when available
      redirectLocation(result) mustBe Some(routes.DummyController.onPageLoad.url)
    }
  }
}
