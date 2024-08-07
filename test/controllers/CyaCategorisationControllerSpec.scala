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
import models.AssessmentAnswer.NoExemption
import models.helper.CategorisationJourney
import models.{AssessmentAnswer, Category1, CategoryRecord, RecordCategorisations, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{AssessmentPage, HasSupplementaryUnitPage, LongerCommodityCodePage, SupplementaryUnitPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.LongerCommodityQuery
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, LongerCommodityCodeSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

import scala.concurrent.Future

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val shortCommodity                = "654321"
  private val unchangedCategoryInfo         =
    categoryQuery.copy(commodityCode = shortCommodity, originalCommodityCode = Some(shortCommodity))
  private val unchangedCommodity            = RecordCategorisations(
    Map(testRecordId -> unchangedCategoryInfo)
  )
  private val previouslyUpdatedCategoryInfo =
    categoryQuery.copy(commodityCode = shortCommodity + 1234, originalCommodityCode = Some(shortCommodity))
  private val previouslyUpdatedCommodity    = RecordCategorisations(
    Map(testRecordId -> previouslyUpdatedCategoryInfo)
  )

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

        "must return a SummaryListRow with the correct supplementary unit and measurement unit appended" in {

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
            val row = SupplementaryUnitSummary.row(userAnswers, testRecordId).value

            val supplementaryValue = row.value.content match {
              case Text(innerContent) => innerContent

            }

            supplementaryValue must be("1234.0 Weight, in kilograms")

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
            .set(RecordCategorisationsQuery, previouslyUpdatedCommodity)
            .success
            .value
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

            expectedLongerCommodityList.rows.size mustEqual 1

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
            .set(RecordCategorisationsQuery, unchangedCommodity)
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

        "if recordcategorisationsquery is empty/none" in {

          val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

          lazy val userAnswersForCategorisationEmptyQuery: UserAnswers = emptyUserAnswers
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption("Y994"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption("NC123"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption("X812"))
            .success
            .value

          val userAnswers = userAnswersForCategorisationEmptyQuery
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1234.0")
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url

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
            when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val sessionRepository = mock[SessionRepository]
            when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .overrides(bind[SessionRepository].toInstance(sessionRepository))
                .build()

            val expectedPayload = CategoryRecord(
              testEori,
              testRecordId,
              None,
              category = 1,
              categoryAssessmentsWithExemptions = 0,
              measurementUnit = categoryQuery.measurementUnit
            )

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onSubmit(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url

              verify(mockConnector)
                .updateCategoryAndComcodeForGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(
                  any()
                )

              withClue("audit event has been fired") {
                verify(mockAuditService)
                  .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(0), eqTo(1))(any)
              }
              withClue("must cleanse the user answers data") {
                verify(sessionRepository).clearData(eqTo(userAnswers.id), eqTo(CategorisationJourney))
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
            when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            val expectedPayload = CategoryRecord(
              testEori,
              testRecordId,
              Some(categoryQuery.commodityCode),
              category = 1,
              categoryAssessmentsWithExemptions = 0,
              measurementUnit = categoryQuery.measurementUnit
            )

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url

              verify(mockConnector)
                .updateCategoryAndComcodeForGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(
                  any()
                )

              withClue("audit event has been fired") {
                verify(mockAuditService)
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
            when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
              .thenReturn(Future.failed(new RuntimeException(":(")))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
                .overrides(bind[AuditService].toInstance(mockAuditService))
                .build()

            val expectedPayload = CategoryRecord(
              testEori,
              testRecordId,
              None,
              category = 1,
              categoryAssessmentsWithExemptions = 0,
              measurementUnit = categoryQuery.measurementUnit
            )

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1)
                .url

              withClue("connector was called even though audit failed") {
                verify(mockConnector)
                  .updateCategoryAndComcodeForGoodsRecord(eqTo(testEori), eqTo(testRecordId), eqTo(expectedPayload))(
                    any()
                  )
              }
            }
          }

        }

      }

      "when user answers cannot update a goods record" - {

        "must not submit anything, and redirect to Journey Recovery" in {

          val mockConnector = mock[GoodsRecordConnector]
          val continueUrl   = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
              .overrides(bind[SessionRepository].toInstance(sessionRepository))
              .build()

          running(application) {
            val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
              .url
            verify(mockConnector, never()).updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any())
            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
            }
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

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .overrides(bind[AuditService].toInstance(mockAuditService))
            .overrides(bind[SessionRepository].toInstance(sessionRepository))
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("audit event has been fired even though connector failed") {
            verify(mockAuditService)
              .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(0), eqTo(1))(any)
          }
          withClue("must not cleanse the user answers data when connector fails") {
            verify(sessionRepository, times(0)).clearData(eqTo(userAnswers.id), eqTo(CategorisationJourney))
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
