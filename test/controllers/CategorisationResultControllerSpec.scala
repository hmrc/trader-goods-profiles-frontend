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
import models.{Category1NoExemptionsScenario, Category1Scenario, Category2Scenario, StandardGoodsNoAssessmentsScenario, StandardGoodsScenario}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CategorisationResultView

class CategorisationResultControllerSpec extends SpecBase {

  "CategorisationResult Controller2" - {

    "must return OK and the correct view for a GET" - {

      "Category1" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, Category1Scenario).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Category1Scenario)(
            request,
            messages(application)
          ).toString
        }
      }

      "Category2" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, Category2Scenario).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Category2Scenario)(
            request,
            messages(application)
          ).toString
        }
      }

      "Standard" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId, StandardGoodsScenario).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, StandardGoodsScenario)(
            request,
            messages(application)
          ).toString
        }
      }

      "StandardNoAssessments" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              routes.CategorisationResultController.onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario).url
            )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, StandardGoodsNoAssessmentsScenario)(
            request,
            messages(application)
          ).toString
        }
      }

      "Category1NoExemptions" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              routes.CategorisationResultController.onPageLoad(testRecordId, Category1NoExemptionsScenario).url
            )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, Category1NoExemptionsScenario)(
            request,
            messages(application)
          ).toString
        }
      }

    }
  }

}
