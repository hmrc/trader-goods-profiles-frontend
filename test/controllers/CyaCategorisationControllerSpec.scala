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
import models.helper.CategorisationJourney
import models.ott.CategorisationInfo
import models.{AssessmentAnswer, Category1Scenario, CategoryRecord, ReassessmentAnswer, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, LongerCommodityCodeSummary, SupplementaryUnitSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CyaCategorisationView

import java.time.Instant
import scala.concurrent.Future

class CyaCategorisationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val onwardRoute   = Call("GET", "/foo")
  private val fakeNavigator = new FakeNavigator(onwardRoute)

  "CyaCategorisationController" - {

    val record = goodsRecordResponse(
      Instant.parse("2022-11-18T23:20:19Z"),
      Instant.parse("2022-11-18T23:20:19Z")
    ).copy(recordId = testRecordId, eori = testEori)

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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2, isReassessmentAnswer = false)
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
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.NoExemption)
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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1, isReassessmentAnswer = false)
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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2, isReassessmentAnswer = false)
                  .get
              )
            )

            val supplementaryUnitRow          = SupplementaryUnitSummary.row(userAnswers, testRecordId)
            val expectedSupplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(userAnswers, testRecordId),
                supplementaryUnitRow
              ).flatten
            )

            withClue("should append measurement unit to supplementary unit") {
              val supplementaryValue = supplementaryUnitRow.get.value.content match {
                case Text(innerContent) => innerContent
                case _                  => ""
              }
              supplementaryValue must be("1234.0 Weight, in kilograms")
            }

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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 2, isReassessmentAnswer = false)
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

        "when longer commodity code, show reassessment answers and longer commodity code" in {

          val longerCat   = categorisationInfo.copy("9876543210", longerCode = true)
          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "987654"))
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value
            .set(LongerCategorisationDetailsQuery(testRecordId), longerCat)
            .success
            .value
            .set(LongerCommodityCodePage(testRecordId), "3210")
            .success
            .value
            .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.Exemption))
            .success
            .value
            .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NoExemption))
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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = true)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category2, 1, isReassessmentAnswer = true)
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

        "use the category assessments that need to be answered list, not the category assessment list" in {

          val categorisationInfo = CategorisationInfo(
            "1234567890",
            "BV",
            None,
            Seq(category1, category2, category3),
            Seq(category1, category3),
            None,
            1
          )

          val userAnswers: UserAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
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
                  .row(testRecordId, userAnswers, category1, 0, isReassessmentAnswer = false)
                  .get,
                AssessmentsSummary
                  .row(testRecordId, userAnswers, category3, 1, isReassessmentAnswer = false)
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
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
              .url
          }
        }

        "when no answers are found" in {
          val application = applicationBuilder(Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
              .url

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

        "if categorisation query is not defined" in {

          val userAnswersForCategorisationEmptyQuery: UserAnswers = emptyUserAnswers
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption)
            .success
            .value
            .set(HasSupplementaryUnitPage(testRecordId), true)
            .success
            .value
            .set(SupplementaryUnitPage(testRecordId), "1234.0")
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisationEmptyQuery)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
              .url

          }
        }

        "when validation errors" in {

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
            .success
            .value
            .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController
              .onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
              .url
          }
        }
      }
    }

    "for a POST" - {

      val expectedCategoryRecord = CategoryRecord(
        testEori,
        testRecordId,
        "1234567890",
        Category1Scenario,
        Some("Weight, in kilograms"),
        None,
        categorisationInfo,
        0,
        wasSupplementaryUnitAsked = false
      )
      "when user answers can update a valid goods record" - {

        "must update the goods record and redirect to the CategorisationResultController with correct view" - {

          "when audit service works" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)
            when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val mockCategorisationService = mock[CategorisationService]
            when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[Navigator].toInstance(fakeNavigator),
                  bind[CategorisationService].toInstance(mockCategorisationService)
                )
                .build()

            val expectedPayload = expectedCategoryRecord

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onSubmit(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockConnector)
                .updateCategoryAndComcodeForGoodsRecord(
                  eqTo(testEori),
                  eqTo(testRecordId),
                  eqTo(expectedPayload)
                )(
                  any()
                )

              withClue("audit event has been fired") {
                verify(mockAuditService)
                  .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(expectedPayload))(any)
              }
              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(CategorisationJourney))
              }

            }
          }

          "when audit service fails" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            val mockConnector = mock[GoodsRecordConnector]
            when(mockConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)
            when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
              .thenReturn(Future.successful(Done))

            val mockAuditService = mock[AuditService]
            when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any())(any()))
              .thenReturn(Future.failed(new RuntimeException(":(")))

            val mockCategorisationService = mock[CategorisationService]
            when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[Navigator].toInstance(fakeNavigator),
                  bind[CategorisationService].toInstance(mockCategorisationService)
                )
                .build()

            val expectedPayload = expectedCategoryRecord

            running(application) {
              val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              withClue("connector was called even though audit failed") {
                verify(mockConnector)
                  .updateCategoryAndComcodeForGoodsRecord(
                    eqTo(testEori),
                    eqTo(testRecordId),
                    eqTo(expectedPayload)
                  )(
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
              .onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
              .url

            verify(mockConnector, never()).updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any())
            withClue("must cleanse the user answers data") {
              verify(sessionRepository).clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
            }
          }
        }
      }

      "must let the play error handler deal with connector failure" in {

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector failed")))

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditFinishCategorisation(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val mockCategorisationService = mock[CategorisationService]
        when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[SessionRepository].toInstance(sessionRepository),
              bind[CategorisationService].toInstance(mockCategorisationService)
            )
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.CyaCategorisationController.onPageLoad(testRecordId).url)

          intercept[RuntimeException] {
            await(route(application, request).value)
          }

          withClue("audit event has been fired even though connector failed") {

            verify(mockAuditService)
              .auditFinishCategorisation(eqTo(testEori), any, eqTo(testRecordId), eqTo(expectedCategoryRecord))(any)
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
