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

import models._
import pages._
import play.api.mvc.Call
import controllers.routes
import controllers.newUkims.{routes => newUkimsRoutes}
import pages.newUkims.{NewUkimsNumberPage, UkimsNumberChangePage}
import pages.profile.CyaNewUkimsNumberPage
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.{Inject, Singleton}

@Singleton
class NewUkimsNavigator @Inject() extends Navigator {

  private val continueUrl = RedirectUrl(newUkimsRoutes.UkimsNumberChangeController.onPageLoad().url)

  val normalRoutes: Page => UserAnswers => Call = {
    case UkimsNumberChangePage => _ => newUkimsRoutes.NewUkimsNumberController.onPageLoad(NormalMode)
    case NewUkimsNumberPage    => _ => newUkimsRoutes.CyaNewUkimsNumberController.onPageLoad()
    case CyaNewUkimsNumberPage => _ => routes.HomePageController.onPageLoad()
    case _                     => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case UkimsNumberChangePage => _ => newUkimsRoutes.NewUkimsNumberController.onPageLoad(CheckMode)
    case NewUkimsNumberPage    => _ => newUkimsRoutes.CyaNewUkimsNumberController.onPageLoad()
    case CyaNewUkimsNumberPage => _ => newUkimsRoutes.NewUkimsNumberController.onPageLoad(CheckMode)
    case _                     => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
  }
}
