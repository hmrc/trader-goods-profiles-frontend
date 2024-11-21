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

package controllers.categorisation

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import connectors.GoodsRecordConnector
import models.helper.CategorisationUpdate
import models.ott.CategorisationInfo
import models.{Category2Scenario, CategoryRecord, Commodity, NormalMode, StandardGoodsNoAssessmentsScenario, UserAnswers}
import navigation.{CategorisationNavigator, FakeCategorisationNavigator}
import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{HasCorrectGoodsLongerCommodityCodePage, LongerCommodityCodePage}
import pages.categorisation.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.Instant
import scala.concurrent.Future
import scala.util.{Failure, Success}

class CategorisationPreparationControllerSpec extends SpecBase with BeforeAndAfterEach {
  private def onwardRoute = Call("GET", "/foo")

  private val mockCategorisationService = mock[CategorisationService]
  private val mockGoodsRecordConnector  = mock[GoodsRecordConnector]
  private val mockSessionRepository     = mock[SessionRepository]
  private val mockAuditService          = mock[AuditService]

  private val longerCommodity = Commodity(
    "1234567890",
    List("Class level1 desc", "Class level2 desc", "Class level3 desc"),
    Instant.now,
    None
  )

  private val categoryInfoNoAssessments = CategorisationInfo(
    "1234567890",
    "BV",
    Some(validityEndDate),
    Seq.empty,
    Seq.empty,
    None,
    1
  )

  private def application(userAnswers: UserAnswers = emptyUserAnswers) =
    applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[CategorisationService].toInstance(mockCategorisationService),
        bind[SessionRepository].toInstance(mockSessionRepository),
        bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
        bind[CategorisationNavigator].toInstance(new FakeCategorisationNavigator(onwardRoute)),
        bind[AuditService].toInstance(mockAuditService)
      )
      .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockCategorisationService)
    reset(mockGoodsRecordConnector)
    reset(mockSessionRepository)
    reset(mockAuditService)

    when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(goodsRecordResponse()))

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
    when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Done))

  }

  "startCategorisation" - {

    "call the categorisation service to get the categorisation info" - {

      "and not update the record if there are questions to answer" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val app = application()

        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockCategorisationService)
            .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId), eqTo(false))(any())

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(CategorisationDetailsQuery(testRecordId)).get mustBe categorisationInfo

            finalUserAnswers.get(LongerCategorisationDetailsQuery(testRecordId)) mustBe None
            finalUserAnswers.get(LongerCommodityCodePage(testRecordId)) mustBe None
            finalUserAnswers.get(HasCorrectGoodsLongerCommodityCodePage(testRecordId)) mustBe None
          }

          withClue("must not get category result from categorisation service as not needed") {
            verify(mockCategorisationService, never())
              .calculateResult(any(), any(), any())
          }

          withClue("must not have updated goods record") {
            verify(mockGoodsRecordConnector, never())
              .updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(
                any()
              )
          }

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categorisationInfo))
              )(any())
          }

          withClue("must not call the audit service finish categorisation event") {
            verify(mockAuditService, never()).auditFinishCategorisation(
              any(),
              any(),
              any(),
              any()
            )(any())
          }

        }

      }

      "and not update the record if no assessments that need answers and commodity is expired" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfoWithEmptyCatAssessThatNeedAnswersWithExpiredCommodityCode)
        )

        val app = application()

        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockCategorisationService)
            .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId), eqTo(false))(any())

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers
              .get(CategorisationDetailsQuery(testRecordId))
              .get mustBe categorisationInfoWithEmptyCatAssessThatNeedAnswersWithExpiredCommodityCode
          }

          withClue("must not get category result from categorisation service as not needed") {
            verify(mockCategorisationService, never())
              .calculateResult(any(), any(), any())
          }

          withClue("must not have updated goods record") {
            verify(mockGoodsRecordConnector, never())
              .updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(
                any()
              )
          }

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categorisationInfoWithEmptyCatAssessThatNeedAnswersWithExpiredCommodityCode))
              )(any())
          }

          withClue("must not call the audit service finish categorisation event") {
            verify(mockAuditService, never()).auditFinishCategorisation(
              any(),
              any(),
              any(),
              any()
            )(any())
          }

        }

      }

      "and update the record if there are no questions to answer" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val app = application()

        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId), eqTo(false))(any())
          }

          withClue("must get category result from categorisation service") {
            verify(mockCategorisationService, times(2))
              .calculateResult(eqTo(categoryInfoNoAssessments), any(), eqTo(testRecordId))
          }

          val categoryRecordArgCaptor: ArgumentCaptor[CategoryRecord] =
            ArgumentCaptor.forClass(classOf[CategoryRecord])
          verify(mockGoodsRecordConnector).updateCategoryAndComcodeForGoodsRecord(
            any(),
            eqTo(testRecordId),
            categoryRecordArgCaptor.capture(),
            any()
          )(any())

          val categoryRecord = categoryRecordArgCaptor.getValue
          categoryRecord.category mustBe StandardGoodsNoAssessmentsScenario
          categoryRecord.assessmentsAnswered mustBe 0

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(CategorisationDetailsQuery(testRecordId)).get mustBe categoryInfoNoAssessments
          }

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categoryInfoNoAssessments))
              )(any())
          }

          withClue("must call the audit service finish categorisation event") {
            verify(mockAuditService).auditFinishCategorisation(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(testRecordId),
              eqTo(categoryRecord)
            )(any())
          }

        }

      }

    }

    "must redirect to Journey Recovery" - {

      "when goods record connector fails" in {

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when categorisation service fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when session repository fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )
        when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categorisationInfo))
              )(any())
          }

        }

      }

      "when goods record connector update fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException(":(")))

        val app = application()
        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categoryInfoNoAssessments))
              )(any())
          }

          withClue("must call the audit service finish categorisation event even though the update failed") {
            verify(mockAuditService).auditFinishCategorisation(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(testRecordId),
              any()
            )(any())
          }

        }

      }

      "when category record fails to build" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        val userAnswersThatWontBuild = emptyUserAnswers
          .set(SupplementaryUnitPage(testRecordId), "1234")
          .success
          .value

        val app = application(userAnswersThatWontBuild)
        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startCategorisation(testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          withClue("must call the audit service start categorisation event") {
            verify(mockAuditService)
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(CategorisationUpdate),
                eqTo(testRecordId),
                eqTo(Some(categoryInfoNoAssessments))
              )(any())
          }

        }

      }

    }

  }

  "startLongerCategorisation" - {

    "call the categorisation service to get the categorisation info" - {

      "and save the category information" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val shorterCommodity = categorisationInfo.copy(commodityCode = "123456")

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))
        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("1234567890"), eqTo("GB"), eqTo(testRecordId), eqTo(true))(any())
          }

          withClue("must set up the reassessment answers") {
            verify(mockCategorisationService).updatingAnswersForRecategorisation(
              any(),
              eqTo(testRecordId),
              eqTo(shorterCommodity),
              eqTo(categorisationInfo)
            )
          }

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            //Capture from here because this routine is mocked and returns stubbed data
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(LongerCategorisationDetailsQuery(testRecordId)).get mustBe categorisationInfo
          }

          withClue("must not get category result from categorisation service as not needed") {
            verify(mockCategorisationService, never())
              .calculateResult(any(), any(), any())
          }

          withClue("must not have updated goods record") {
            verify(mockGoodsRecordConnector, never())
              .updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(
                any()
              )
          }

        }

      }

      "and update the goods record if nothing to answer" in {

        val longerCode = categoryInfoNoAssessments.copy(longerCode = true)
        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(longerCode)
        )

        val shorterCommodity = categorisationInfo.copy(commodityCode = "123456")

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("1234567890"), eqTo("GB"), eqTo(testRecordId), eqTo(true))(any())
          }

          withClue("must set up the reassessment answers") {
            verify(mockCategorisationService).updatingAnswersForRecategorisation(
              any(),
              eqTo(testRecordId),
              eqTo(shorterCommodity),
              eqTo(longerCode)
            )
          }

          withClue("must get category result from categorisation service") {
            verify(mockCategorisationService, times(2))
              .calculateResult(any(), any(), any())
          }

          val categoryRecordArgCaptor: ArgumentCaptor[CategoryRecord] =
            ArgumentCaptor.forClass(classOf[CategoryRecord])

          verify(mockGoodsRecordConnector).updateCategoryAndComcodeForGoodsRecord(
            any(),
            eqTo(testRecordId),
            categoryRecordArgCaptor.capture(),
            any()
          )(any())

          val categoryRecord = categoryRecordArgCaptor.getValue
          categoryRecord.category mustBe StandardGoodsNoAssessmentsScenario
          categoryRecord.assessmentsAnswered mustBe 0

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            //Capture from here because this routine is mocked and returns stubbed data
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(LongerCategorisationDetailsQuery(testRecordId)).get mustBe longerCode
          }

          withClue("must call the audit service finish categorisation event") {
            verify(mockAuditService).auditFinishCategorisation(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(testRecordId),
              eqTo(categoryRecord)
            )(any())
          }

        }

      }

      "and should not update the record if there is a supplementary question to answer" in {

        val longerCodeWithMeasurementUnit =
          categoryInfoNoAssessments.copy(longerCode = true).copy(measurementUnit = Some("KG"))
        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(longerCodeWithMeasurementUnit)
        )

        val shorterCommodity = categorisationInfo.copy(commodityCode = "123456")

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(Category2Scenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("1234567890"), eqTo("GB"), eqTo(testRecordId), eqTo(true))(any())
          }

          withClue("must get category result from categorisation service") {
            verify(mockCategorisationService)
              .calculateResult(any(), any(), any())
          }

          withClue("must not call the audit service finish categorisation event") {
            verify(mockAuditService, never()).auditFinishCategorisation(any(), any(), any(), any())(any())
          }
        }

      }

      "and preserve the supplementary unit if it has not changed between short and long code" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val shorterCommodity = categorisationInfo.copy(commodityCode = "123456")

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value
          .set(HasSupplementaryUnitPage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), "1234")
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must keep supplementary unit in answers") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(HasSupplementaryUnitPage(testRecordId)) mustBe Some(true)
            finalUserAnswers.get(SupplementaryUnitPage(testRecordId)) mustBe Some("1234")

          }

        }

      }

      "and preserve the supplementary unit if it has changed between previous longer code and current longer code" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val shorterCommodity     = categorisationInfo.copy(commodityCode = "123456")
        val firstLongerCommodity =
          categorisationInfo.copy(commodityCode = "1234566666")

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(LongerCategorisationDetailsQuery(testRecordId), firstLongerCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value
          .set(HasSupplementaryUnitPage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), "1234")
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must keep supplementary unit in answers") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(HasSupplementaryUnitPage(testRecordId)) mustBe Some(true)
            finalUserAnswers.get(SupplementaryUnitPage(testRecordId)) mustBe Some("1234")

          }

        }

      }

      "and remove the supplementary unit if it has changed between short and long code" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val shorterCommodity = categorisationInfo.copy(commodityCode = "123456", measurementUnit = Some("kg"))

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value
          .set(HasSupplementaryUnitPage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), "1234")
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must remove supplementary unit from answer") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            //Capture from here because this routine is mocked and returns stubbed data
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(HasSupplementaryUnitPage(testRecordId)) mustBe None
            finalUserAnswers.get(SupplementaryUnitPage(testRecordId)) mustBe None

          }

        }

      }

      "and remove the supplementary unit if it has changed between previous longer and current long code" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo)
        )

        val shorterCommodity     = categorisationInfo.copy(commodityCode = "123456", measurementUnit = Some("kg"))
        val firstLongerCommodity =
          categorisationInfo.copy(commodityCode = "1234566666", measurementUnit = Some("litres"))
        val userAnswers          = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
          .success
          .value
          .set(LongerCategorisationDetailsQuery(testRecordId), firstLongerCommodity)
          .success
          .value
          .set(
            LongerCommodityQuery(testRecordId),
            longerCommodity
          )
          .success
          .value
          .set(HasSupplementaryUnitPage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), "1234")
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(Success(userAnswers))

        when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
          .thenReturn(Future.successful(userAnswers))

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must remove supplementary unit from answer") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            //Capture from here because this routine is mocked and returns stubbed data
            verify(mockCategorisationService)
              .updatingAnswersForRecategorisation(uaArgCaptor.capture(), any(), any(), any())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(LongerCategorisationDetailsQuery(testRecordId)) mustBe Some(categorisationInfo)
            finalUserAnswers.get(HasSupplementaryUnitPage(testRecordId)) mustBe None
            finalUserAnswers.get(SupplementaryUnitPage(testRecordId)) mustBe None

          }

        }

      }

      "and save the category information without copying answers from previous assessments" - {

        "when the longer commodity code was already set to the same value" in {

          when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
            Future.successful(categorisationInfo)
          )

          val shorterCommodity = categorisationInfo.copy(commodityCode = "123456")

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), shorterCommodity)
            .success
            .value
            .set(
              LongerCommodityQuery(testRecordId),
              longerCommodity
            )
            .success
            .value
            .set(
              LongerCategorisationDetailsQuery(testRecordId),
              categorisationInfo
            )
            .success
            .value

          when(mockCategorisationService.reorderRecategorisationAnswers(any(), any()))
            .thenReturn(Future.successful(userAnswers))

          val app = application(userAnswers)
          running(app) {

            val request =
              FakeRequest(
                GET,
                controllers.categorisation.routes.CategorisationPreparationController
                  .startLongerCategorisation(NormalMode, testRecordId)
                  .url
              )
            val result  = route(app, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            withClue("must get category details from categorisation service") {
              verify(mockCategorisationService)
                .getCategorisationInfo(any(), eqTo("1234567890"), eqTo("GB"), eqTo(testRecordId), eqTo(true))(any())
            }

            withClue("must not update answers from categorisation service as not needed") {
              verify(mockCategorisationService, never())
                .updatingAnswersForRecategorisation(any(), any(), any(), any())
            }

            withClue("must not have updated goods record") {
              verify(mockGoodsRecordConnector, never())
                .updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(
                  any()
                )
            }

          }
        }
      }
    }

    "must redirect to Journey Recovery" - {

      "when goods record connector fails" in {

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when categorisation service get info fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when categorisation details query is not set" in {

        val app = application()

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when longer commodity query is not set" in {

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value

        val app = application(userAnswers)

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when updating answers for reassessment call fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(
            Failure(new Exception(":("))
          )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(LongerCommodityQuery(testRecordId), longerCommodity)
          .success
          .value

        val app = application(userAnswers)
        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when goods record connector update fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(LongerCommodityQuery(testRecordId), longerCommodity)
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(
            Success(userAnswers)
          )
        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException(":(")))

        val app = application(userAnswers)
        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url

          withClue("must call the audit service finish categorisation event even though the update failed") {
            verify(mockAuditService).auditFinishCategorisation(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(testRecordId),
              any()
            )(any())
          }
        }

      }

      "when category record fails to build" in {

        when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("error")))

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(LongerCommodityQuery(testRecordId), longerCommodity)
          .success
          .value
          .set(SupplementaryUnitPage(testRecordId), "543")
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(
            Success(userAnswers)
          )

        val app = application(userAnswers)
        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when session repository fails" in {

        when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("error")))

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
          .success
          .value
          .set(LongerCommodityQuery(testRecordId), longerCommodity)
          .success
          .value

        when(mockCategorisationService.updatingAnswersForRecategorisation(any(), any(), any(), any()))
          .thenReturn(
            Success(userAnswers)
          )

        val app = application(userAnswers)

        running(app) {
          val request =
            FakeRequest(
              GET,
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
                .url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

      }

    }

  }
}
