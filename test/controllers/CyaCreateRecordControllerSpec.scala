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
import base.TestConstants.testEori
import connectors.GoodsRecordConnector
import models.{GoodsRecord, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers._
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCreateRecordView

import scala.concurrent.Future

class CyaCreateRecordControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCreateRecordController" - {

    def createChangeList(userAnswers: UserAnswers, app: Application): SummaryList = SummaryListViewModel(
      rows = Seq(
        TraderReferenceSummary.row(userAnswers)(messages(app)),
        UseTraderReferenceSummary.row(userAnswers)(messages(app)),
        GoodsDescriptionSummary.row(userAnswers)(messages(app)),
        CountryOfOriginSummary.row(userAnswers)(messages(app)),
        CommodityCodeSummary.row(userAnswers)(messages(app))
      ).flatten
    )

    "for a GET" - {

      "must return OK and the correct view with valid mandatory data" in {

        val userAnswers = mandatoryRecordUserAnswers

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaCreateRecordView]
          val list = createChangeList(userAnswers, application)

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
          val list = createChangeList(userAnswers, application)

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

      "when user answers can create a valid goods record" - {

        "must submit the goods record and redirect to the CreateRecordSuccessController" in {

          val userAnswers = mandatoryRecordUserAnswers

          val mockConnector = mock[GoodsRecordConnector]
          when(mockConnector.submitGoodsRecordUrl(any(), any())(any())).thenReturn(Future.successful(Done))

          val mockAuditService = mock[AuditService]
          when(mockAuditService.auditFinishCreateGoodsRecord(any(), any(), any())(any()))
            .thenReturn(Future.successful(Done))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

            val result = route(application, request).value

            val expectedPayload = GoodsRecord(
              testEori,
              "123",
              testCommodity.commodityCode,
              "1",
              "123",
              "1",
              testCommodity.validityStartDate
            )

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CreateRecordSuccessController.onPageLoad().url
            verify(mockConnector, times(1)).submitGoodsRecordUrl(eqTo(expectedPayload), eqTo(testEori))(any())

            withClue("must call the audit connector with the supplied details") {
              verify(mockAuditService, times(1))
                .auditFinishCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual), eqTo(userAnswers))(any())
            }
          }
        }
      }

      "when user answers cannot create a goods record" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector    = mock[GoodsRecordConnector]
          val mockAuditService = mock[AuditService]
          val continueUrl      = RedirectUrl(routes.CreateRecordStartController.onPageLoad().url)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .overrides(bind[AuditService].toInstance(mockAuditService))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
            verify(mockConnector, never()).submitGoodsRecordUrl(any(), any())(any())

            withClue("must not try and submit an audit") {
              verify(mockAuditService, never()).auditFinishCreateGoodsRecord(any(), any(), any())(any())
            }
          }

        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = mandatoryRecordUserAnswers

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.submitGoodsRecordUrl(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }
        }
      }

      "must let the play error handler deal with an audit future failure" in {

        val userAnswers = mandatoryRecordUserAnswers

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.submitGoodsRecordUrl(any(), any())(any())).thenReturn(Future.successful(Done))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditFinishCreateGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Audit failed")))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCreateRecordController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
