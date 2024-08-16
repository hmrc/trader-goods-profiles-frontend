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
import base.TestConstants.testRecordId
import connectors.GoodsRecordConnector
import models.ott.CategorisationInfo2
import models.{CategoryRecord2, Commodity, NormalMode, StandardGoodsNoAssessmentsScenario, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery2, LongerCategorisationDetailsQuery, LongerCommodityQuery2}
import repositories.SessionRepository
import services.CategorisationService

import java.time.Instant
import scala.concurrent.Future

class CategorisationPreparationControllerSpec extends SpecBase with BeforeAndAfterEach {
  private def onwardRoute = Call("GET", "/foo")

  private val mockCategorisationService = mock[CategorisationService]
  private val mockGoodsRecordConnector  = mock[GoodsRecordConnector]
  private val mockSessionRepository     = mock[SessionRepository]

  private def application(userAnswers: UserAnswers = emptyUserAnswers) =
    applicationBuilder(userAnswers = Some(userAnswers))
      .overrides(
        bind[CategorisationService].toInstance(mockCategorisationService),
        bind[SessionRepository].toInstance(mockSessionRepository),
        bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockCategorisationService, mockGoodsRecordConnector, mockSessionRepository)

    when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(goodsRecordResponse()))

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

  }

  "startCategorisation" - {

    "call the categorisation service to get the categorisation info" - {

      "and not update the record if there are questions to answer" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo2)
        )

        val app = application()

        running(app) {

          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockCategorisationService)
            .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId), eqTo(false))(any())

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(CategorisationDetailsQuery2(testRecordId)).get mustBe categorisationInfo2
          }

          withClue("must not get category result from categorisation service as not needed") {
            verify(mockCategorisationService, times(0))
              .calculateResult(any(), any(), any())
          }

          withClue("must not have updated goods record") {
            verify(mockGoodsRecordConnector, times(0)).updateCategoryAndComcodeForGoodsRecord2(any(), any(), any())(
              any()
            )
          }

        }

      }

      "and update the record if there are no questions to answer" in {

        val categoryInfoNoAssessments = CategorisationInfo2(
          "1234567890",
          Seq.empty,
          Seq.empty,
          None,
          1
        )

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord2(any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val app = application()

        running(app) {

          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId), eqTo(false))(any())
          }

          withClue("must get category result from categorisation service") {
            verify(mockCategorisationService)
              .calculateResult(eqTo(categoryInfoNoAssessments), any(), eqTo(testRecordId))
          }

          withClue("must have updated goods record") {
            val categoryRecordArgCaptor: ArgumentCaptor[CategoryRecord2] =
              ArgumentCaptor.forClass(classOf[CategoryRecord2])
            verify(mockGoodsRecordConnector).updateCategoryAndComcodeForGoodsRecord2(
              any(),
              eqTo(testRecordId),
              categoryRecordArgCaptor.capture()
            )(any())

            val categoryRecord = categoryRecordArgCaptor.getValue
            categoryRecord.category mustBe StandardGoodsNoAssessmentsScenario
            categoryRecord.categoryAssessmentsWithExemptions mustBe 0
          }

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(CategorisationDetailsQuery2(testRecordId)).get mustBe categoryInfoNoAssessments
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
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when categorisation service fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when session repository fails" in {

        when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("error")))

        val app = application()

        running(app) {
          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when goods record connector update fails" in {

        val categoryInfoNoAssessments = CategorisationInfo2(
          "1234567890",
          Seq.empty,
          Seq.empty,
          None,
          1
        )

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        when(mockCategorisationService.calculateResult(any(), any(), any()))
          .thenReturn(StandardGoodsNoAssessmentsScenario)

        when(mockGoodsRecordConnector.updateCategoryAndComcodeForGoodsRecord2(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException(":(")))

        val app = application()
        running(app) {
          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when category record fails to build" in {

        val categoryInfoNoAssessments = CategorisationInfo2(
          "1234567890",
          Seq.empty,
          Seq.empty,
          None,
          1
        )

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categoryInfoNoAssessments)
        )

        val app = application()
        running(app) {
          val request =
            FakeRequest(GET, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

    }

  }

  "startLongerCategorisation" - {

    "call the categorisation service to get the categorisation info" - {

      "and save the category information" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any(), any())(any())).thenReturn(
          Future.successful(categorisationInfo2)
        )

        val userAnswers = emptyUserAnswers
          .set(
            LongerCommodityQuery2(testRecordId),
            Commodity(
              "1234567890",
              List("Class level1 desc", "Class level2 desc", "Class level3 desc"),
              Instant.now,
              None
            )
          )
          .success
          .value

        val app = application(userAnswers)
        running(app) {

          val request =
            FakeRequest(
              GET,
              routes.CategorisationPreparationController.startLongerCategorisation(NormalMode, testRecordId).url
            )
          val result  = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must get category details from categorisation service") {
            verify(mockCategorisationService)
              .getCategorisationInfo(any(), eqTo("1234567890"), eqTo("GB"), eqTo(testRecordId), eqTo(true))(any())
          }

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(LongerCategorisationDetailsQuery(testRecordId)).get mustBe categorisationInfo2
          }

        }

      }

    }

  }
}
