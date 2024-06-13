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
import models.helper.{Category1, Category2, StandardNoSupplementaryUnits}
import models.router.responses.GetGoodsRecordResponse
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CategorisationResultView
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.inject.bind

import scala.concurrent.Future

class CategorisationResultControllerSpec extends SpecBase {

  "CategorisationResult Controller" - {

    "must return OK and the correct view for a GET" - {

      "Category1" in {

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.getRecord(eqTo(testEori), eqTo(testRecordId))(any()))
          .thenReturn(
            Future.successful(
              GetGoodsRecordResponse(
                testRecordId,
                "10410100",
                "EC",
                1,
                None
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(Category1)(request, messages(application)).toString
        }
      }

      "Category2" in {

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.getRecord(eqTo(testEori), eqTo(testRecordId))(any()))
          .thenReturn(
            Future.successful(
              GetGoodsRecordResponse(
                testRecordId,
                "10410100",
                "EC",
                2,
                None
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(Category2)(request, messages(application)).toString
        }
      }

      "StandardNoSupplementaryUnits" in {

        val mockConnector = mock[GoodsRecordConnector]
        when(mockConnector.getRecord(eqTo(testEori), eqTo(testRecordId))(any()))
          .thenReturn(
            Future.successful(
              GetGoodsRecordResponse(
                testRecordId,
                "10410100",
                "EC",
                3,
                None
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CategorisationResultView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(StandardNoSupplementaryUnits)(request, messages(application)).toString
        }
      }
    }

    "must error if the goods record has supplementary units and is category 1" in {

      val mockConnector = mock[GoodsRecordConnector]
      when(mockConnector.getRecord(eqTo(testEori), eqTo(testRecordId))(any()))
        .thenReturn(
          Future.successful(
            GetGoodsRecordResponse(
              testRecordId,
              "10410100",
              "EC",
              3,
              Some(1)
            )
          )
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[GoodsRecordConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategorisationResultController.onPageLoad(testRecordId).url)

        intercept[Exception] {
          await(route(application, request).value)
        }
      }
    }
  }
}
