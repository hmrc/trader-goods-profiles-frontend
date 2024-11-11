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
import controllers.routes
import models.GoodsRecordsPagination.firstPage
import models._
import org.scalatest.BeforeAndAfterEach
import pages.Page
import pages.goodsProfile.{PreviousMovementRecordsPage, RemoveGoodsRecordPage}

class GoodsProfileNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val navigator = new GoodsProfileNavigator()

  "CategorisationNavigator" - {

    "must go from RemoveGoodsRecordPage to page 1 of GoodsRecordsController" in {
      navigator.nextPage(
        RemoveGoodsRecordPage,
        NormalMode,
        emptyUserAnswers
      ) mustEqual controllers.goodsProfile.routes.GoodsRecordsController
        .onPageLoad(firstPage)
    }

    "must go from PreviousMovementsRecordsPage to page 1 of the GoodsRecordController" in {
      navigator.nextPage(
        PreviousMovementRecordsPage,
        NormalMode,
        emptyUserAnswers
      ) mustEqual controllers.goodsProfile.routes.GoodsRecordsController
        .onPageLoad(firstPage)
    }

    "return IndexController.onPageLoad for other pages in normalRoutes" in {
      val page = new Page {}

      navigator.normalRoutes(page)(emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
    }

  }
}
