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
import base.TestConstants.userAnswersId
import models.{Commodity, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCreateRecordView

class CyaCreateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCreateProfileController" - {

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val answers = mandatoryRecordUserAnswers

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = SummaryListViewModel(
            rows = Seq.empty
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view with all data (including optional)" in {

        val userAnswers = fullRecordUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = SummaryListViewModel(
            rows = Seq.empty
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(routes.CreateRecordStartController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "must redirect to ???" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
        }
      }
    }
  }
}
