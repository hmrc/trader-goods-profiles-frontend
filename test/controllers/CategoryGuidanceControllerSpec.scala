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
import connectors.OttConnector
import models.Commodity
import models.ott.response.{GoodsNomenclatureResponse, OttResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CommodityQuery
import repositories.SessionRepository
import views.html.CategoryGuidanceView

import scala.concurrent.Future

class CategoryGuidanceControllerSpec extends SpecBase {

  "CategoryGuidance Controller" - {

    val userAnswersWithCommodity = emptyUserAnswers
      .set(
        CommodityQuery,
        Commodity(commodityCode = "123", description = "test commodity")
      )
      .success
      .value

    val mockOttConnector = mock[OttConnector]
    when(mockOttConnector.getCategorisationInfo(any())(any())).thenReturn(
      Future.successful(
        OttResponse(
          GoodsNomenclatureResponse("", ""),
          Seq(),
          Seq()
        )
      )
    )

    "must call OTT and save the response in user answers prior to loading on a GET" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(mockOttConnector, times(1)).getCategorisationInfo(any())(any())
        verify(mockSessionRepository, times(1)).set(any())

      }
    }

    "must redirect to Journey Recover when no commodity query has been provided" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockOttConnector, never()).getCategorisationInfo(any())(any())
      }
    }

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CategoryGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to the categorisation page when the user click continue button" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[OttConnector].toInstance(mockOttConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        // TODO replace index route
        redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
      }
    }
  }
}
