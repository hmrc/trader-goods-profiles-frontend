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
import models.UserAnswers
import models.ott.CategoryAssessment
import models.router.responses.GetGoodsRecordResponse
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CategorisationDetailsQuery2
import repositories.SessionRepository
import services.CategorisationService

import java.time.Instant
import scala.concurrent.Future

class CategorisationPreparationControllerSpec extends SpecBase with BeforeAndAfterEach {
  private def onwardRoute = Call("GET", "/foo")

  private val mockCategorisationService = mock[CategorisationService]
  private val mockGoodsRecordConnector  = mock[GoodsRecordConnector]
  private val mockSessionRepository     = mock[SessionRepository]

  private def application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[CategorisationService].toInstance(mockCategorisationService),
      bind[SessionRepository].toInstance(mockSessionRepository),
      bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
      bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
    )
    .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any())(any())).thenReturn(
      Future.successful(categorisationInfo2)
    )
    when(mockGoodsRecordConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(goodsRecordResponse()))

    when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

  }

  "startCategorisation" - {

    "when there are questions to answer" - {

      "call the categorisation service to get the categorisation info" in {

        running(application) {

          val request =
            FakeRequest(POST, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockCategorisationService)
            .getCategorisationInfo(any(), eqTo("12345678"), eqTo("GB"), eqTo(testRecordId))(any())

          withClue("must update User Answers with Categorisation Info") {
            val uaArgCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository).set(uaArgCaptor.capture())

            val finalUserAnswers = uaArgCaptor.getValue

            finalUserAnswers.get(CategorisationDetailsQuery2(testRecordId)).get mustBe categorisationInfo2
          }

        }

      }

    }

    "must redirect to Journey Recovery" - {

      "when goods record connector fails" in {

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        running(application) {
          val request =
            FakeRequest(POST, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when categorisation service fails" in {

        when(mockCategorisationService.getCategorisationInfo(any(), any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("error")))

        running(application) {
          val request =
            FakeRequest(POST, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

      "when session repository fails" in {

        when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("error")))

        running(application) {
          val request =
            FakeRequest(POST, routes.CategorisationPreparationController.startCategorisation(testRecordId).url)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

      }

    }

  }
}
