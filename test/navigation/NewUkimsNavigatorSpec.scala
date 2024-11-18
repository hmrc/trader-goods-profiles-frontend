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
import controllers.newUkims.{routes => newUkimsRoutes}
import models._
import org.scalatest.BeforeAndAfterEach
import pages._
import pages.newUkims.{NewUkimsNumberPage, UkimsNumberChangePage}
import pages.profile._
import play.api.http.Status.SEE_OTHER
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class NewUkimsNavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val navigator = new NewUkimsNavigator()

  "NewUkimsNavigator" - {

    "when in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "within the new UKIMS number update journey" - {

        "must go from UkimsNumberChangePage to NewUkimsNumberController" in {
          navigator.nextPage(
            UkimsNumberChangePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe newUkimsRoutes.NewUkimsNumberController.onPageLoad(NormalMode)
        }

        "must go from NewUkimsNumberPage to CyaNewUkimsNumberController" in {
          navigator.nextPage(
            NewUkimsNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe newUkimsRoutes.CyaNewUkimsNumberController.onPageLoad
        }

        "must go from CyaNewUkimsNumberPage to HomePageController" in {
          navigator.nextPage(
            CyaNewUkimsNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.HomePageController.onPageLoad()
        }
      }
    }

    "when in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to Journey Recovery" in {
        val continueUrl = RedirectUrl(newUkimsRoutes.UkimsNumberChangeController.onPageLoad().url)
        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
      }

      "must go from UkimsNumberChangePage to NewUkimsNumberController" in {
        navigator.nextPage(
          UkimsNumberChangePage,
          CheckMode,
          emptyUserAnswers
        ) mustBe newUkimsRoutes.NewUkimsNumberController.onPageLoad(CheckMode)
      }

      "must go from NewUkimsNumberPage to CyaNewUkimsNumberController" in {
        navigator.nextPage(
          NewUkimsNumberPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe newUkimsRoutes.CyaNewUkimsNumberController.onPageLoad
      }
    }

    ".journeyRecovery" - {

      "redirect to JourneyRecovery" - {

        "with no ContinueUrl if none supplied" in {
          val result = navigator.journeyRecovery()
          result.header.status mustEqual SEE_OTHER
          result.header
            .headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }

        "with ContinueUrl if one supplied" in {
          val redirectUrl = Some(RedirectUrl("/redirectUrl"))
          val result      = navigator.journeyRecovery(redirectUrl)
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(redirectUrl)
            .url
        }
      }
    }
  }
}
