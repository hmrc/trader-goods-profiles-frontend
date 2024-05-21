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

        "to CyaNirmsNiphl when answer is No" in {

          val answers = UserAnswers("id").set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.CyaNirmsNiphlController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CyaNirmsNiphl" in {

        navigator.nextPage(
          NiphlNumberPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.CyaNirmsNiphlController.onPageLoad
      }

      "must go from HasGoodsDescriptionPage" - {

        "to GoodsDescriptionPage when answer is Yes" in {

          val answers = UserAnswers("id").set(HasGoodsDescriptionPage, true).success.value
          navigator.nextPage(HasGoodsDescriptionPage, NormalMode, answers) mustBe routes.GoodsDescriptionController
            .onPageLoad(
              NormalMode
            )
        }

        "to CountryOfOriginPage when answer is No" in {

          val answers = UserAnswers("id").set(HasGoodsDescriptionPage, false).success.value
          navigator.nextPage(HasGoodsDescriptionPage, NormalMode, answers) mustBe routes.CountryOfOriginController
            .onPageLoad(NormalMode)
        }

        "to JourneyRecoveryPage when answer is not present" in {

          val answers = UserAnswers("id")
          navigator.nextPage(HasGoodsDescriptionPage, NormalMode, answers) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "must go from GoodsDescriptionPage to CountryOfOriginPage" in {
        navigator.nextPage(
          GoodsDescriptionPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.CountryOfOriginController.onPageLoad(
          NormalMode
        )
      }

      "must go from CountryOfOriginPage to CommodityCodePage" in {
        navigator.nextPage(
          CountryOfOriginPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.CommodityCodeController.onPageLoad(
          NormalMode
        )
      }

      "must go from CommodityCodePage to HasCorrectGoodsPage" in {

        navigator.nextPage(
          CommodityCodePage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.HasCorrectGoodsController.onPageLoad(NormalMode)
      }

      "must go from HasCorrectGoodsPage to CyaCreateRecord" in {

        navigator.nextPage(
          HasCorrectGoodsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.CyaCreateRecordController.onPageLoad
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CyaNirmsNiphl" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CyaNirmsNiphlController.onPageLoad
      }

      "must go from UkimsNumberPage to CyaNirmsNiphl" in {

        navigator.nextPage(
          UkimsNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CyaNirmsNiphlController.onPageLoad
      }

      "must go from HasNirmsPage" - {

        "when answer is Yes" - {

          "to NirmsNumberPage when NirmsNumberPage is empty" in {

            val answers = UserAnswers("id").set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CyaNirmsNiphl when NirmsNumberPage is answered" in {

            val answers =
              UserAnswers("id").set(HasNirmsPage, true).success.value.set(NirmsNumberPage, "1234").success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaNirmsNiphlController.onPageLoad
          }
        }
        "to CyaNirmsNiphl when answer is No" in {

          val answers = UserAnswers("id").set(HasNirmsPage, false).success.value
          navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaNirmsNiphlController.onPageLoad
        }
      }

      "must go from NirmsNumberPage to CyaNirmsNiphl" in {

        navigator.nextPage(
          NirmsNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CyaNirmsNiphlController.onPageLoad
      }

      "must go from HasNiphlPage" - {

        "when answer is Yes" - {

          "to NiphlNumberPage when NiphlNumberPage is empty" in {

            val answers = UserAnswers("id").set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
              CheckMode
            )
          }

          "to CyaNirmsNiphl when NiphlNumberPage is answered" in {

            val answers =
              UserAnswers("id").set(HasNiphlPage, true).success.value.set(NiphlNumberPage, "1234").success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaNirmsNiphlController.onPageLoad
          }
        }

        "to CyaNirmsNiphl when answer is No" in {

          val answers = UserAnswers("id").set(HasNiphlPage, false).success.value
          navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaNirmsNiphlController.onPageLoad
        }
      }

      "must go from NiphlNumberPage to CyaNirmsNiphl" in {

        navigator.nextPage(
          NiphlNumberPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CyaNirmsNiphlController.onPageLoad
      }
    }
  }
}
