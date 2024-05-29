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
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCategorisationController" - {

    "for a GET" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCategorisationView]
          val list = SummaryListViewModel(
            rows = Seq.empty
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "for a POST" - {

        "must redirect to ???" in {

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
          }
        }
      }
    }
  }
}
