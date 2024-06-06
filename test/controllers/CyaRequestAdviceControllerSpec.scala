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
import connectors.AccreditationConnector
import models.UserAnswers
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{EmailSummary, NameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaRequestAdviceView

import scala.concurrent.Future

class CyaRequestAdviceControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaRequestAdviceController" - {

    def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
      rows = Seq(
        NameSummary.row(userAnswers)(messages(app)),
        EmailSummary.row(userAnswers)(messages(app))
      ).flatten
    )

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val userAnswers = mandatoryAdviceUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaRequestAdviceController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaRequestAdviceView]
          val list = createChangeList(userAnswers, application)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        val continueUrl = RedirectUrl(routes.AdviceStartController.onPageLoad().url)

        running(application) {
          val request = FakeRequest(GET, routes.CyaRequestAdviceController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaRequestAdviceController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

//      "must submit the advice request and redirect to AdviceSuccessController" in {
//
//        val userAnswers = mandatoryProfileUserAnswers
//
//        val mockConnector = mock[AccreditationConnector]
//        when(mockConnector.submitRequestAccreditation(any())(any())).thenReturn(Future.successful(Done))
//
//        val application =
//          applicationBuilder(userAnswers = Some(userAnswers))
//            .overrides(bind[AccreditationConnector].toInstance(mockConnector))
//            .build()
//
//        running(application) {
//          val request = FakeRequest(POST, routes.CyaRequestAdviceController.onPageLoad.url)
//
//          val result = route(application, request).value
//
//          status(result) mustEqual SEE_OTHER
//          redirectLocation(result).value mustEqual routes.AdviceSuccessController.onPageLoad().url
//        }
//      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatoryAdviceUserAnswers

        val mockConnector = mock[AccreditationConnector]
        when(mockConnector.submitRequestAccreditation(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[AccreditationConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaRequestAdviceController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaRequestAdviceController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }
}
