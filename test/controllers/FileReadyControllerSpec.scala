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
import views.html.FileReadyView

class FileReadyControllerSpec extends SpecBase with MockitoSugar {

  private lazy val fileReadyRoute = routes.FileReadyController.onPageLoad.url

  "FileReadyController" - {

    "onPageLoad" - {

      "must OK and display correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        // TODO: Update these with mocks.
        val fileSizeKilobytes = 1024
        val fileDownloadLink  = "www.example.com"
        val createdDate       = "19 July 2024"
        val availableUntil    = "18 August 2024"

        running(application) {
          val request = FakeRequest(GET, fileReadyRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[FileReadyView]
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(fileSizeKilobytes, fileDownloadLink, createdDate, availableUntil)(
            request,
            messages(application)
          ).toString
        }

      }

    }

  }
}
