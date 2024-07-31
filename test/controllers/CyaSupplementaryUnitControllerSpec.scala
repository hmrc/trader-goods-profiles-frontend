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
import models.helper.SupplementaryUnitUpdateJourney
import models.{NormalMode, SupplementaryRequest}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaSupplementaryUnitView

import scala.concurrent.Future

class CyaSupplementaryUnitControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaSupplementaryUnitController" - {

    "for a GET" - {
      val emptySummaryList = SummaryListViewModel(
        rows = Seq.empty
      )

      "must return OK and the correct view with valid data" in {

        val application                      = applicationBuilder(userAnswers = Some(mandatorySupplementaryUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)

        running(application) {
          val request = FakeRequest(GET, routes.CyaSupplementaryUnitController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaSupplementaryUnitView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.rowUpdate(mandatorySupplementaryUserAnswers, testRecordId),
                SupplementaryUnitSummary.rowUpdate(mandatorySupplementaryUserAnswers, testRecordId)
              ).flatten
            ),
            testRecordId
          )(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery if no answers are found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaSupplementaryUnitController.onPageLoad("recordId").url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for a POST" - {

      "when user answers can create a valid supplementary request" - {

        "must submit the supplementary request and redirect to the SingleRecordController and cleanse userAnswers" in {

          val userAnswers = mandatorySupplementaryUserAnswers

          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[SessionRepository].toInstance(sessionRepository)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaSupplementaryUnitController.onSubmit(testRecordId).url)

            val result = route(application, request).value

            val expectedPayload = SupplementaryRequest(
              eori = testEori,
              recordId = testRecordId,
              hasSupplementaryUnit = Some(true),
              supplementaryUnit = Some("100"),
              measurementUnit = Some("litres")
            )

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SingleRecordController.onPageLoad(testRecordId).url
            verify(mockGoodsRecordConnector)
              .updateSupplementaryUnitForGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(any())

            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
            }
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatorySupplementaryUserAnswers

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.updateSupplementaryUnitForGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[SessionRepository].toInstance(sessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaSupplementaryUnitController.onSubmit("recordId").url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("must not cleanse the user answers data when connector fails") {
            verify(sessionRepository, times(0)).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaSupplementaryUnitController.onSubmit("recordId").url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
