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
import connectors.GoodsRecordConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import utils.GetRecordsResponseUtil
import views.html.PreviousMovementRecordsView

import scala.concurrent.Future

class PreviousMovementRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  "PreviousMovementRecords Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsconnector.doRecordsExist(any())(any())) thenReturn Future.successful(
        mockGetRecordsResponseOption
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousMovementRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
        verify(mockGetGoodsRecordsconnector, times(1)).doRecordsExist(any())(any())
      }
    }
    // TODO change it to actual controller (goods page without records) when available
    "must skip page when records not available" in {

      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsconnector.doRecordsExist(any())(any())) thenReturn Future.successful(
        None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockGetGoodsRecordsconnector, times(1)).doRecordsExist(any())(any())
      }
    }

    "must skip page when records are empty" in {

      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsconnector.doRecordsExist(any())(any())) thenReturn Future.successful(
        mockGetRecordsEmpty
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockGetGoodsRecordsconnector, times(1)).doRecordsExist(any())(any())
      }
    }

    "must redirect to the next page on load record" in {

      val mockSessionRepository = mock[SessionRepository]

      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockGetGoodsRecordsconnector.getRecords(any())(any())) thenReturn Future.successful(
        mockGetRecordsResponse
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PreviousMovementRecordsController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        //TODO change it to actual when available
        redirectLocation(result).value mustEqual routes.HomePageController.onPageLoad().url

        verify(mockGetGoodsRecordsconnector, times(1)).getRecords(any())(any())
      }
    }
  }
}
