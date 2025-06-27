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

package navigation

import base.SpecBase
import base.TestConstants.userAnswersId
import config.FrontendAppConfig
import controllers.routes
import models.GoodsRecordsPagination.firstPage
import models.*
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.Page
import pages.goodsProfile.{GoodsRecordsPage, PreviousMovementRecordsPage, RemoveGoodsRecordPage}

class GoodsProfileNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val mockFrontendAppConfig = mock[FrontendAppConfig]
  private val navigator             = new GoodsProfileNavigator(mockFrontendAppConfig)

  "GoodsProfileNavigator" - {
    "must go from RemoveGoodsRecordPage" - {
      val searchFormData = SearchForm(
        searchTerm = Some("bananas"),
        statusValue = Seq.empty,
        countryOfOrigin = None
      )

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value

      "to page 1 of GoodsRecordsController.onPageLoadFilter when there is a GoodsRecordSearchFilter applied and enhancedSearch is true" in {
        when(mockFrontendAppConfig.enhancedSearch) thenReturn true

        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, userAnswers) mustEqual
          controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(firstPage)
      }

      "to page 1 of GoodsRecordsSearchResultController when there is a GoodsRecordSearchFilter applied and enhancedSearch is false" in {
        when(mockFrontendAppConfig.enhancedSearch) thenReturn false

        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, userAnswers) mustEqual
          controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(firstPage)
      }

      "to page 1 of GoodsRecordsController when there is no GoodsRecordSearchFilter applied" in {
        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, emptyUserAnswers) mustEqual
          controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
      }
    }

    "must go from PreviousMovementsRecordsPage to page 1 of the GoodsRecordController" in {
      navigator.nextPage(PreviousMovementRecordsPage, NormalMode, emptyUserAnswers) mustEqual
        controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
    }

    "return IndexController.onPageLoad for other pages in normalRoutes" in {
      val page = new Page {}

      navigator.normalRoutes(page)(emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
    }
  }
}
