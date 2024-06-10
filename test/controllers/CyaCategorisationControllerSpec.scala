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
import base.TestConstants.{testEori, testRecordId}
import connectors.GoodsRecordConnector
import models.CategoryRecord
import models.router.responses.CreateGoodsRecordResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

import scala.concurrent.Future

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCategorisationController" - {

    "for a GET" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCategorisationView]
          val list = SummaryListViewModel(
            rows = Seq.empty
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list, testRecordId)(request, messages(application)).toString
        }
      }

      "for a POST" - {

        "when user answers can update a valid goods record" - {

          "must update the goods record and redirect to the CyaCategorisationController" in {

            val userAnswers = mandatoryAssessmentAnswers

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateGoodsRecord(any())(any()))
              .thenReturn(Future.successful(CreateGoodsRecordResponse(testRecordId)))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              val expectedPayload = CategoryRecord(
                eori = testEori,
                recordId = testRecordId,
                category = Some(1),
                measurementUnit = Some("1")
              )

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId)
                .url
              verify(mockConnector, times(1)).updateGoodsRecord(eqTo(expectedPayload))(any())
            }
          }
        }

        "when user answers cannot update a goods record" - {

          "must not submit anything, and redirect to Journey Recovery" in {

            val mockConnector = mock[GoodsRecordConnector]
            val continueUrl   = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController
                .onPageLoad(Some(continueUrl))
                .url
              verify(mockConnector, never()).updateGoodsRecord(any())(any())
            }

          }
        }

        "must let the play error handler deal with connector failure" in {

          val userAnswers = mandatoryAssessmentAnswers

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.updateGoodsRecord(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("Connector failed")))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            intercept[RuntimeException] {
              await(route(application, request).value)
            }
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
