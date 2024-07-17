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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.GoodsRecordConnector
import models.{AssessmentAnswer, Category1, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import models.AssessmentAnswer.NoExemption
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import pages.{AssessmentPage, HasSupplementaryUnitPage, LongerCommodityCodePage, SupplementaryUnitPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
import services.AuditService
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, LongerCommodityCodeSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

import scala.concurrent.Future

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaCategorisationController" - {

    "for a GET" - {

      val emptySummaryList = SummaryListViewModel(
        rows = Seq.empty
      )

      "must return OK and the correct view" - {

        "when all category assessments answered" in {

          val userAnswers = userAnswersForCategorisation

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2)
                  .get
              )
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              emptySummaryList,
              emptySummaryList
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "when no exemption is used, meaning some assessment pages are not answered" in {

          val userAnswers = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), NoExemption)
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get
              )
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              emptySummaryList,
              emptySummaryList
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "when supplementary unit is supplied" in {

          val userAnswers = userAnswersForCategorisation
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1234.0")
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2)
                  .get
              )
            )

            val expectedSupplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(userAnswers, testRecordId),
                SupplementaryUnitSummary.row(userAnswers, testRecordId)
              ).flatten
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              expectedSupplementaryUnitList,
              emptySummaryList
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "when supplementary unit is not supplied" in {

          val userAnswers = userAnswersForCategorisation
            .set(HasSupplementaryUnitPage(testRecordId), false)
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CyaCategorisationView]

            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2)
                  .get
              )
            )

            val expectedSupplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(userAnswers, testRecordId)
              ).flatten
            )
            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              expectedSupplementaryUnitList,
              emptySummaryList
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "when longer commodity code is given" in {

          val userAnswers = userAnswersForCategorisation
            .set(LongerCommodityCodePage(testRecordId), "1234")
            .success
            .value

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2)
                  .get
              )
            )

            val expectedLongerCommodityList = SummaryListViewModel(
              rows = Seq(
                LongerCommodityCodeSummary.row(userAnswers, testRecordId)
              ).flatten
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              emptySummaryList,
              expectedLongerCommodityList
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "when longer commodity code is not given" in {

          val userAnswers = userAnswersForCategorisation

          val application                      = applicationBuilder(userAnswers = Some(userAnswers)).build()
          implicit val localMessages: Messages = messages(application)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            val view                   = application.injector.instanceOf[CyaCategorisationView]
            val expectedAssessmentList = SummaryListViewModel(
              rows = Seq(
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category1, 0)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2)
                  .get
              )
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              testRecordId,
              expectedAssessmentList,
              emptySummaryList,
              emptySummaryList
            )(
              request,
              messages(application)
            ).toString
          }
        }
      }

      "must redirect to Journey Recovery" - {

        "when no category assessments answered" in {

          val userAnswers = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
          }
        }

        "when no answers are found" in {
          val application = applicationBuilder(Some(emptyUserAnswers)).build()
          val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

          }
        }

        "when no existing data is found" in {
          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when validation errors" in {

          val userAnswers = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "123.0")
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url
          }
        }
      }
    }

    "for a POST" - {

      "when user answers can update a valid goods record" - {

        "must update the goods record and redirect to the CategorisationResultController with correct view" - {

          "when audit service works without longer commodity code" in {

            val userAnswers = UserAnswers(userAnswersId)
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateCategoryForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url
              //TODO update with new expected - Why was this TODO in main?
//              verify(mockConnector, times(1))
//                .updateGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(any())

              withClue("audit event has been fired") {
                verify(mockAuditService, times(1))
                  .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(0), eqTo(1))(any)
              }

            }
          }

          "when audit service works with longer commodity code" in {

            val userAnswers = UserAnswers(userAnswersId)
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(LongerCommodityQuery(testRecordId), testCommodity)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateCategoryForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url
              //TODO update with new expected
//              verify(mockConnector, times(1))
//                .updateGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(any())

              withClue("audit event has been fired") {
                verify(mockAuditService, times(1))
                  .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(0), eqTo(1))(any)
              }

            }
          }

          "when audit service fails" in {

            val userAnswers = UserAnswers(userAnswersId)
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.updateCategoryForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.failed(new RuntimeException(":(")))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url

              withClue("connector was called even though audit failed") {
//TODO what is expected here now??
                //                verify(mockConnector, times(1))
//                  .updateGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(any())
              }
            }
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
            verify(mockConnector, never()).updateCategoryForGoodsRecord(any(), any(), any())(any())
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(RecordCategorisationsQuery, recordCategorisations)
          .success
          .value

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.updateGoodsRecord(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("audit event has been fired even though connector failed") {
            verify(mockAuditService, times(1))
              .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(0), eqTo(1))(any)
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
