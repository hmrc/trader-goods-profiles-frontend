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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Call
import controllers.routes
import pages._
import models._

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case ProfileSetupPage => _ => routes.UkimsNumberController.onPageLoad(NormalMode)
    case UkimsNumberPage => _ => routes.HasNirmsController.onPageLoad(NormalMode)
    case HasNirmsPage => navigateFromHasNirms
    case NirmsNumberPage => _ => routes.HasNiphlController.onPageLoad(NormalMode)
    case HasNiphlPage => navigateFromHasNiphl
    case NiphlNumberPage => _ => routes.CheckYourAnswersController.onPageLoad
    case _ => _ => routes.IndexController.onPageLoad
  }

  private def navigateFromHasNirms(answers: UserAnswers): Call = {
    answers.get(HasNirmsPage).map {
      case true => routes.NirmsNumberController.onPageLoad(NormalMode)
      case false => routes.HasNiphlController.onPageLoad(NormalMode)
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  private def navigateFromHasNiphl(answers: UserAnswers): Call = {
    answers.get(HasNiphlPage).map {
      case true => routes.NiphlNumberController.onPageLoad(NormalMode)
      case false => routes.CheckYourAnswersController.onPageLoad
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case HasNirmsPage => navigateFromHasNirmsCheck
    case HasNiphlPage => navigateFromHasNiphlCheck
    case _ => _ => routes.CheckYourAnswersController.onPageLoad
  }

  private def navigateFromHasNirmsCheck(answers: UserAnswers): Call = {
    answers.get(HasNirmsPage).map {
      case true => if(answers.isDefined(NirmsNumberPage)) {
        routes.CheckYourAnswersController.onPageLoad
      } else {
        routes.NirmsNumberController.onPageLoad(CheckMode)
      }
      case false => routes.CheckYourAnswersController.onPageLoad
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  private def navigateFromHasNiphlCheck(answers: UserAnswers): Call = {
    answers.get(HasNiphlPage).map {
      case true => if(answers.isDefined(NiphlNumberPage)) {
        routes.CheckYourAnswersController.onPageLoad
      } else {
        routes.NiphlNumberController.onPageLoad(CheckMode)
      }
      case false => routes.CheckYourAnswersController.onPageLoad
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
