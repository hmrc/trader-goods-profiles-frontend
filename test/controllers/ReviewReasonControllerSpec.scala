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
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ReviewReasonView

import java.time.Instant
import scala.concurrent.Future

class ReviewReasonControllerSpec extends SpecBase with MockitoSugar {

  private lazy val reviewReasonRoute = routes.ReviewReasonController.onPageLoad(testRecordId).url

  "ReviewReasonController" - {

    val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
    when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

    "onPageLoad" - {

      "must OK and display correct view for each review reason" in {

        val reviewReasons = Seq("mismatch", "inadequate", "unclear", "commodity", "measure")

        reviewReasons.map { reviewReason =>
          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
            .thenReturn(Future.successful(toReviewGoodsRecordResponse(Instant.now, Instant.now, reviewReason)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, reviewReasonRoute)
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[ReviewReasonView]
            status(result) mustEqual OK
            contentAsString(result) mustEqual view(testRecordId, reviewReason)(request, messages(application)).toString

            verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
          }
        }

      }

      "must OK and display correct view for review reason in a different case" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(toReviewGoodsRecordResponse(Instant.now, Instant.now, "Inadequate")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, reviewReasonRoute)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[ReviewReasonView]
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(testRecordId, "inadequate")(request, messages(application)).toString

          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }

      }

      "must redirect to SingleRecordController when the record is not marked with 'toReview'" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, reviewReasonRoute)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

      "must redirect to JourneyRecovery when there is an issue getting the goods record" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]

        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Something went wrong")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, reviewReasonRoute)
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockGoodsRecordConnector).getRecord(any(), any())(any())
        }
      }

    }

    "onSubmit" - {

      "must redirect to SingleRecordController when the user clicks the continue button" in {

        val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
          .overrides(
            bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, reviewReasonRoute)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url
        }
      }

    }
  }
}
