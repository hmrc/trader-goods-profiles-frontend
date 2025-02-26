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

package controllers.newUkims

import base.SpecBase
import models.NormalMode
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.newUkims.UkimsNumberChangeView

class UkimsNumberChangeControllerSpec extends SpecBase {

  "UkimsNumberChangeController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.newUkims.routes.UkimsNumberChangeController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UkimsNumberChangeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to NewUkimsNumberController for a POST" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.newUkims.routes.UkimsNumberChangeController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.newUkims.routes.NewUkimsNumberController
          .onPageLoad(NormalMode)
          .url
      }
    }
  }
}
