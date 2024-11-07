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

package controllers.goodsProfile

import base.SpecBase
import base.TestConstants.testEori
import connectors.GoodsRecordConnector
import controllers.routes
import models.RecordsSummary
import models.RecordsSummary.Update
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.GoodsRecordsLoadingView

import java.time.Instant
import scala.concurrent.Future

class GoodsRecordsLoadingControllerSpec extends SpecBase {

  "GoodsRecordsLoading Controller" - {

    val continueUrl = routes.GoodsRecordsController.onPageLoad(1).url

    "must return OK and the correct view for a GET if it's updating" in {

      val recordsToStore = 15
      val recordsStored  = 5

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecordsSummary(any())(any())) thenReturn Future
        .successful(RecordsSummary(testEori, Some(Update(recordsStored, recordsToStore)), Instant.now))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .configure(
          "goods-records-loading-page.refresh-rate" -> 3
        )
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.GoodsRecordsLoadingController.onPageLoad(Some(RedirectUrl(continueUrl))).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsLoadingView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(recordsStored, recordsToStore, Some(RedirectUrl(continueUrl)))(
          request,
          messages(application)
        ).toString
        header("Refresh", result).value mustEqual "3"
      }
    }

    "must redirect to previous page if it has finished updating" in {

      val mockGoodsRecordConnector = mock[GoodsRecordConnector]

      when(mockGoodsRecordConnector.getRecordsSummary(any())(any())) thenReturn Future
        .successful(RecordsSummary(testEori, None, Instant.now))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.GoodsRecordsLoadingController.onPageLoad(Some(RedirectUrl(continueUrl))).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual continueUrl
      }
    }
  }
}
