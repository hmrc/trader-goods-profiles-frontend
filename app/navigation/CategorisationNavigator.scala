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

import controllers.routes
import models._
import pages._
import pages.categorisation.CategoryGuidancePage
import play.api.mvc.Call
import services.CategorisationService
import utils.Constants.firstAssessmentNumber

import javax.inject.{Inject, Singleton}

@Singleton
class CategorisationNavigator @Inject() (categorisationService: CategorisationService) extends NavigatorTrait {

  val normalRoutes: Page => UserAnswers => Call = {
    case p: CategoryGuidancePage =>
      _ => routes.AssessmentController.onPageLoad(NormalMode, p.recordId, firstAssessmentNumber)
    case _                       => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = { case _ =>
    _ => routes.JourneyRecoveryController.onPageLoad()
  }

}