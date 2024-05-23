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
import pages._
import models._

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad
      }

      "must go from ProfileSetupPage to UkimsNumberPage" in {

        navigator.nextPage(ProfileSetupPage, NormalMode, emptyUserAnswers) mustBe routes.UkimsNumberController
          .onPageLoad(NormalMode)
      }

      "must go from UkimsNumberPage to HasNirmsPage" in {

        navigator.nextPage(UkimsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNirmsController.onPageLoad(
          NormalMode
        )
      }

      "must go from HasNirmsPage" - {

        "to NirmsNumberPage when answer is Yes" in {

          val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
          navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
            NormalMode
          )
        }

        "to HasNiphlPage when answer is No" in {

          val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
          navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.HasNiphlController.onPageLoad(NormalMode)
        }
      }

      "must go from NirmsNumberPage to HasNiphlPage" in {

        navigator.nextPage(NirmsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNiphlController.onPageLoad(
          NormalMode
        )
      }

      "must go from HasNiphlPage" - {

        "to NiphlNumberPage when answer is Yes" in {

          val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
          navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
            NormalMode
          )
        }

        "to CyaCreateProfile when answer is No" in {

          val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CyaCreateProfile" in {

        navigator.nextPage(
          NiphlNumberPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateProfileController.onPageLoad
      }

      "must go from HasGoodsDescriptionPage" - {

        "to GoodsDescriptionPage when answer is Yes" in {

          val answers = UserAnswers(userAnswersId).set(HasGoodsDescriptionPage, true).success.value
          navigator.nextPage(HasGoodsDescriptionPage, NormalMode, answers) mustBe routes.GoodsDescriptionController
            .onPageLoad(
              NormalMode
            )
        }

        "to CountryOfOriginPage when answer is No" in {

          val answers = UserAnswers(userAnswersId).set(HasGoodsDescriptionPage, false).success.value
          navigator.nextPage(HasGoodsDescriptionPage, NormalMode, answers) mustBe routes.CountryOfOriginController
            .onPageLoad(NormalMode)
        }

        "to JourneyRecoveryPage when answer is not present" in {

          navigator.nextPage(
            HasGoodsDescriptionPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "must go from GoodsDescriptionPage to CountryOfOriginPage" in {
        navigator.nextPage(
          GoodsDescriptionPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.CountryOfOriginController.onPageLoad(
          NormalMode
        )
      }

      "must go from CountryOfOriginPage to CommodityCodePage" in {
        navigator.nextPage(
          CountryOfOriginPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.CommodityCodeController.onPageLoad(
          NormalMode
        )
      }

      "must go from CommodityCodePage to HasCorrectGoodsPage" in {

        navigator.nextPage(
          CommodityCodePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.HasCorrectGoodsController.onPageLoad(NormalMode)
      }

      "must go from HasCorrectGoodsPage to CyaCreateRecord" in {

        navigator.nextPage(
          HasCorrectGoodsPage,
          NormalMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateRecordController.onPageLoad
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CyaCreateProfile" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateProfileController.onPageLoad
      }

      "must go from UkimsNumberPage to CyaCreateProfile" in {

        navigator.nextPage(
          UkimsNumberPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateProfileController.onPageLoad
      }

      "must go from HasNirmsPage" - {

        "when answer is Yes" - {

          "to NirmsNumberPage when NirmsNumberPage is empty" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CyaCreateProfile when NirmsNumberPage is answered" in {

            val answers =
              UserAnswers(userAnswersId)
                .set(HasNirmsPage, true)
                .success
                .value
                .set(NirmsNumberPage, "1234")
                .success
                .value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
          }
        }
        "to CyaCreateProfile when answer is No" in {

          val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
          navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
        }
      }

      "must go from NirmsNumberPage to CyaCreateProfile" in {

        navigator.nextPage(
          NirmsNumberPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateProfileController.onPageLoad
      }

      "must go from HasNiphlPage" - {

        "when answer is Yes" - {

          "to NiphlNumberPage when NiphlNumberPage is empty" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CyaCreateProfile when NiphlNumberPage is answered" in {

            val answers =
              UserAnswers(userAnswersId)
                .set(HasNiphlPage, true)
                .success
                .value
                .set(NiphlNumberPage, "1234")
                .success
                .value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
          }
        }

        "to CyaCreateProfile when answer is No" in {

          val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CyaCreateProfile" in {

        navigator.nextPage(
          NiphlNumberPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.CyaCreateProfileController.onPageLoad
      }
    }
  }
}
