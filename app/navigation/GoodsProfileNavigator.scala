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
import models.GoodsRecordsPagination.firstPage
import models.UserAnswers
import pages.Page
import pages.goodsProfile.{GoodsRecordsPage, PreviousMovementRecordsPage, RemoveGoodsRecordPage}
import play.api.mvc.Call
import config.FrontendAppConfig

import javax.inject.{Inject, Singleton}

@Singleton
class GoodsProfileNavigator @Inject() (appConfig: FrontendAppConfig) extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case PreviousMovementRecordsPage =>
      _ => controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
    case RemoveGoodsRecordPage       => navigateFromRemoveGoodsRecordPage
    case _                           => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = { case _ =>
    _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

//  private def navigateFromRemoveGoodsRecordPage(answers: UserAnswers): Call =
//    if (searchFilterIsApplied(answers)) {
//      controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(firstPage)
//    } else {
//      controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
//    }

  private def navigateFromRemoveGoodsRecordPage(answers: UserAnswers): Call =
    if (searchFilterIsApplied(answers) && (appConfig.enhancedSearch)) {
      controllers.goodsProfile.routes.GoodsRecordsController.onPageLoadFilter(firstPage)
    } else if (searchFilterIsApplied(answers) && (!appConfig.enhancedSearch)) {
      controllers.goodsProfile.routes.GoodsRecordsSearchResultController.onPageLoad(firstPage)
    } else {
      controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(firstPage)
    }

  private def searchFilterIsApplied(answers: UserAnswers): Boolean =
    answers.get(GoodsRecordsPage) match {
      case Some(searchText) => true
      case None             => false
    }
}
