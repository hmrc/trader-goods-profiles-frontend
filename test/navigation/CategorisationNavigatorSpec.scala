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
import models._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.Page
import pages.categorisation.CategoryGuidancePage
import services.CategorisationService
import utils.Constants

class CategorisationNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val categorisationService = mock[CategorisationService]

  private val navigator = new CategorisationNavigator(categorisationService)

  private val recordId    = "dummyRecordId"
  private val userAnswers = UserAnswers(recordId)

  "CategorisationNavigator" - {

    "return AssessmentController.onPageLoad for CategoryGuidancePage in normalRoutes" in {

      navigator.normalRoutes(CategoryGuidancePage(recordId))(
        userAnswers
      ) mustBe controllers.categorisation.routes.AssessmentController
        .onPageLoad(NormalMode, recordId, Constants.firstAssessmentNumber)
    }

    "return IndexController.onPageLoad for other pages in normalRoutes" in {
      val page = new Page {}

      navigator.normalRoutes(page)(userAnswers) mustBe routes.IndexController.onPageLoad()
    }

    "when in Check mode" - {}

  }
}
