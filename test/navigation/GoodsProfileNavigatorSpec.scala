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
import controllers.routes
import models.*
import models.GoodsRecordsPagination.firstPage
import org.scalatest.BeforeAndAfterEach
import pages.Page
import pages.goodsProfile.{GoodsRecordsPage, RemoveGoodsRecordPage}

class GoodsProfileNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val navigator = new GoodsProfileNavigator

  "GoodsProfileNavigator" - {
    "must go from RemoveGoodsRecordPage" - {
      val searchFormData = SearchForm(
        searchTerm = Some("bananas"),
        statusValue = Seq.empty,
        countryOfOrigin = None
      )

      val userAnswers = UserAnswers(userAnswersId).set(GoodsRecordsPage, searchFormData).success.value

      "to page 1 of GoodsRecordsController.onPageLoadFilter when there is a GoodsRecordSearchFilter applied " in {

        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, userAnswers) mustEqual
          controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(firstPage)
      }

      "to page 1 of GoodsRecordsController when there is no GoodsRecordSearchFilter applied" in {
        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, emptyUserAnswers) mustEqual
          controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
      }
    }

    "return IndexController.onPageLoad for other pages in normalRoutes" in {
      val page = new Page {}

      navigator.normalRoutes(page)(emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
    }
  }
}
