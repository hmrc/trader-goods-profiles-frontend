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
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import models.GoodsRecordsPagination.firstPage
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.GetRecordsResponseUtil
import views.html.PreviousMovementRecordsView

import scala.concurrent.Future

class PreviousMovementRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
  /*
  "PreviousMovementRecords Controller" - {

    "must return OK and the correct view for a GET when this the users first time on this page and they do have records but they have not been stored" in {
      val totalRecords                 = 10
      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsconnector.doRecordsExist(any())(any())) thenReturn Future.successful(false)
      when(mockGetGoodsRecordsconnector.getRecordsCount(any())(any())) thenReturn Future.successful(totalRecords)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousMovementRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
        verify(mockGetGoodsRecordsconnector).doRecordsExist(any())(any())
        verify(mockGetGoodsRecordsconnector).getRecordsCount(any())(any())
      }
    }

    "must redirect to goods list page for a GET when this the users records have been stored" in {
      val totalRecords                 = 10
      val mockGetGoodsRecordsConnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsConnector.doRecordsExist(any())(any())) thenReturn Future.successful(true)
      when(mockGetGoodsRecordsConnector.getRecordsCount(any())(any())) thenReturn Future.successful(totalRecords)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GoodsRecordsController.onPageLoad(firstPage).url
        verify(mockGetGoodsRecordsConnector).doRecordsExist(any())(any())
        verify(mockGetGoodsRecordsConnector).getRecordsCount(any())(any())

      }
    }

    "must redirect to no records page for a GET when this the users first time on this page and they do not have any records" in {

      val totalRecords                 = 0
      val mockGetGoodsRecordsconnector = mock[GoodsRecordConnector]
      when(mockGetGoodsRecordsconnector.getRecordsCount(any())(any())) thenReturn Future.successful(totalRecords)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsconnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousMovementRecordsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GoodsRecordsController.onPageLoadNoRecords().url
        verify(mockGetGoodsRecordsconnector).getRecordsCount(any())(any())
      }
    }

    "must redirect to the next page on load record and store all records" in {

      val mockGetGoodsRecordsConnector = mock[GoodsRecordConnector]

      when(mockGetGoodsRecordsConnector.storeAllRecords(any())(any())) thenReturn Future.successful(Done)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGetGoodsRecordsConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PreviousMovementRecordsController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.GoodsRecordsController.onPageLoad(firstPage).url

        verify(mockGetGoodsRecordsConnector).storeAllRecords(any())(any())
      }
    }
  }

   */
}
