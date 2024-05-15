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
import pages._
import models._

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }

      "must go from ProfileSetupPage to UkimsNumberPage" in {

        navigator.nextPage(ProfileSetupPage, NormalMode, UserAnswers("id")) mustBe routes.UkimsNumberController
          .onPageLoad(NormalMode)
      }

      "must go from UkimsNumberPage to HasNirmsPage" in {

        navigator.nextPage(UkimsNumberPage, NormalMode, UserAnswers("id")) mustBe routes.HasNirmsController.onPageLoad(
          NormalMode
        )
      }

      "must go from HasNirmsPage" - {

        "to NirmsNumberPage when answer is Yes" in {

          val answers = UserAnswers("id").set(HasNirmsPage, true).success.value
          navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
            NormalMode
          )
        }

        "to HasNiphlPage when answer is No" in {

          val answers = UserAnswers("id").set(HasNirmsPage, false).success.value
          navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.HasNiphlController.onPageLoad(NormalMode)
        }
      }

      "must go from NirmsNumberPage to HasNiphlPage" in {

        navigator.nextPage(NirmsNumberPage, NormalMode, UserAnswers("id")) mustBe routes.HasNiphlController.onPageLoad(
          NormalMode
        )
      }

      "must go from HasNiphlPage" - {

        "to NiphlNumberPage when answer is Yes" in {

          val answers = UserAnswers("id").set(HasNiphlPage, true).success.value
          navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
            NormalMode
          )
        }

        "to CheckYourAnswersPage when answer is No" in {

          val answers = UserAnswers("id").set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.CheckYourAnswersController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CheckYourAnswersPage" in {

        navigator.nextPage(
          NiphlNumberPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from UkimsNumberPage to CheckYourAnswersPage" in {

        navigator.nextPage(
          UkimsNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from HasNirmsPage" - {

        "when answer is Yes" - {

          "to NirmsNumberPage when NirmsNumberPage is empty" in {

            val answers = UserAnswers("id").set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CheckYourAnswers when NirmsNumberPage is answered" in {

            val answers =
              UserAnswers("id").set(HasNirmsPage, true).success.value.set(NirmsNumberPage, "1234").success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CheckYourAnswersController.onPageLoad
          }
        }
        "to CheckYourAnswersPage when answer is No" in {

          val answers = UserAnswers("id").set(HasNirmsPage, false).success.value
          navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CheckYourAnswersController.onPageLoad
        }
      }

      "must go from NirmsNumberPage to CheckYourAnswersPage" in {

        navigator.nextPage(
          NirmsNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from HasNiphlPage" - {

        "when answer is Yes" - {

          "to NiphlNumberPage when NiphlNumberPage is empty" in {

            val answers = UserAnswers("id").set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CheckYourAnswers when NiphlNumberPage is answered" in {

            val answers =
              UserAnswers("id").set(HasNiphlPage, true).success.value.set(NiphlNumberPage, "1234").success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CheckYourAnswersController.onPageLoad
          }
        }

        "to CheckYourAnswersPage when answer is No" in {

          val answers = UserAnswers("id").set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CheckYourAnswersController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CheckYourAnswersPage" in {

        navigator.nextPage(
          NiphlNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }
    }
  }
}
