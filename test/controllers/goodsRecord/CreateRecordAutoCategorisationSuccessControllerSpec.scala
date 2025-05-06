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

package controllers.goodsRecord

import base.SpecBase
import base.TestConstants.testRecordId
import connectors.GoodsRecordConnector
import models.router.responses.{GetGoodsRecordResponse, GetRecordsResponse}
import models.{AdviceStatus, DeclarableStatus, GoodsRecordsPagination}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.goodsRecord.CreateRecordAutoCategorisationSuccessView

import java.time.Instant
import scala.concurrent.Future

class CreateRecordAutoCategorisationSuccessControllerSpec extends SpecBase with MockitoSugar {

  "CreateRecordAutoCategorisationSuccessController" - {

    "CreateRecordAutoCategorisationSuccessController" - {

      "for a GET" - {
        "must return OK and the correct view when record is IMMIReady" in {

          val mockConnector = mock[GoodsRecordConnector]

          val mockRecord = GetGoodsRecordResponse(
            recordId = testRecordId,
            eori = "testEori",
            actorId = "actor",
            traderRef = "trader",
            comcode = "12345678",
            adviceStatus = AdviceStatus.AdviceReceived,
            goodsDescription = "desc",
            countryOfOrigin = "GB",
            category = None,
            measurementUnit = None,
            comcodeEffectiveFromDate = Instant.now,
            version = 1,
            active = true,
            toReview = false,
            reviewReason = None,
            declarable = DeclarableStatus.ImmiReady,
            createdDateTime = Instant.now,
            updatedDateTime = Instant.now
          )

          val mockResponse = Some(
            GetRecordsResponse(Seq(mockRecord), GoodsRecordsPagination(1, 1, 1, None, None))
          )

          when(
            mockConnector.searchRecords(
              any(),
              any(),
              any(),
              any(),
              eqTo(Some(true)),
              any(),
              any(),
              any(),
              any()
            )(any())
          ).thenReturn(Future.successful(mockResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .build()

          running(application) {
            val request = FakeRequest(
              GET,
              controllers.goodsRecord.routes.CreateRecordAutoCategorisationSuccessController
                .onPageLoad(testRecordId)
                .url
            )

            val result = route(application, request).value

            val view = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(testRecordId, true)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view when record is not IMMIReady but found with notReadyForIMMI" in {

          val mockConnector = mock[GoodsRecordConnector]

          val mockRecord = GetGoodsRecordResponse(
            recordId = testRecordId,
            eori = "testEori",
            actorId = "actor",
            traderRef = "trader",
            comcode = "12345678",
            adviceStatus = AdviceStatus.AdviceReceived,
            goodsDescription = "desc",
            countryOfOrigin = "GB",
            category = None,
            measurementUnit = None,
            comcodeEffectiveFromDate = Instant.now,
            version = 1,
            active = true,
            toReview = false,
            reviewReason = None,
            declarable = DeclarableStatus.NotReadyForImmi,
            createdDateTime = Instant.now,
            updatedDateTime = Instant.now
          )

          val emptyResponse    = Some(GetRecordsResponse(Seq.empty, GoodsRecordsPagination(1, 0, 0, None, None)))
          val notReadyResponse = Some(GetRecordsResponse(Seq(mockRecord), GoodsRecordsPagination(1, 1, 1, None, None)))

          when(
            mockConnector.searchRecords(
              any(),
              any(),
              any(),
              any(),
              eqTo(Some(true)),
              any(),
              any(),
              any(),
              any()
            )(any())
          ).thenReturn(Future.successful(emptyResponse))

          when(
            mockConnector.searchRecords(
              any(),
              any(),
              any(),
              any(),
              eqTo(None),
              eqTo(Some(true)),
              any(),
              any(),
              any()
            )(any())
          ).thenReturn(Future.successful(notReadyResponse))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
            .build()

          running(application) {
            val request = FakeRequest(
              GET,
              controllers.goodsRecord.routes.CreateRecordAutoCategorisationSuccessController
                .onPageLoad(testRecordId)
                .url
            )

            val result = route(application, request).value

            val view = application.injector.instanceOf[CreateRecordAutoCategorisationSuccessView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(testRecordId, false)(request, messages(application)).toString
          }
        }

      }
    }
  }
}
