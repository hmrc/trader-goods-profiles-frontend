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

package controllers

import base.SpecBase
import play.api.i18n.Lang
import play.api.test.Helpers.*

class LanguageSwitchControllerSpec extends SpecBase {

  "LanguageSwitchController" - {
    "fallbackURL" - {
      "must be the IndexController url" in {
        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        running(application) {
          val languageController = application.injector.instanceOf[LanguageSwitchController]
          languageController.fallbackURL mustBe routes.IndexController.onPageLoad().url
        }
      }
    }

    "languageMap" - {
      "must contain english and welsh entries" in {
        val application = applicationBuilder(Some(emptyUserAnswers)).build()
        running(application) {
          val languageController = application.injector.instanceOf[LanguageSwitchController]
          languageController.languageMap mustBe Map(
            "en" -> Lang("en"),
            "cy" -> Lang("cy")
          )
        }
      }
    }

  }
}
