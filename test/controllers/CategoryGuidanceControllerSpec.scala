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

import base.SpecBase
import base.TestConstants.testRecordId
import navigation.{FakeNavigator, Navigator}
import play.api.inject._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CategoryGuidanceView

class CategoryGuidanceControllerSpec extends SpecBase {

  private val onwardRoute = Call("GET", "/foo")

  "CategoryGuidance Controller" - {

    "onPageLoad should display view" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CategoryGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId)(request, messages(application)).toString
      }
    }

    "onSubmit should call navigator to redirect" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit(testRecordId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

  }

}
