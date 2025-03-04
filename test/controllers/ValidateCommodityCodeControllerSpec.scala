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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.CommodityService

import scala.concurrent.Future

class ValidateCommodityCodeControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockCommodityService: CommodityService = mock[CommodityService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCommodityService)
  }

  "ValidateCommodityCodeController" - {

    "changeCategory" - {
      "must redirect to CategorisationPreparationController when commodity code is valid" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[CommodityService].toInstance(mockCommodityService)
          )
          .build()

        when(mockCommodityService.isCommodityCodeValid(any())(any(), any())).thenReturn(Future.successful(true))

        running(application) {
          val request =
            FakeRequest(GET, controllers.routes.ValidateCommodityCodeController.changeCategory(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.categorisation.routes.CategorisationPreparationController
            .startCategorisation(testRecordId)
            .url
        }
      }
      "must redirect to InvalidCommodityCodePage when commodity code is invalid" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[CommodityService].toInstance(mockCommodityService)
          )
          .build()

        when(mockCommodityService.isCommodityCodeValid(any())(any(), any())).thenReturn(Future.successful(false))

        running(application) {
          val request =
            FakeRequest(GET, controllers.routes.ValidateCommodityCodeController.changeCategory(testRecordId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(
            result
          ).value mustBe controllers.goodsRecord.commodityCode.routes.InvalidCommodityCodeController
            .onPageLoad(testRecordId)
            .url
        }
      }

    }

  }
}
