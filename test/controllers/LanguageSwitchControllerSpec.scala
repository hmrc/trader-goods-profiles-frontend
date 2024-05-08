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
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Lang
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.play.language.LanguageUtils

class LanguageSwitchControllerSpec extends SpecBase {
  lazy val controllerComponents: ControllerComponents = stubControllerComponents()
  private val languageUtils                           = mock[LanguageUtils]

  private val languageSwitchController = new LanguageSwitchController(
    app.injector.instanceOf[FrontendAppConfig],
    languageUtils,
    controllerComponents
  )

  "LanguageSwitchController" - {

    "must return fallbackURL" in {

      val result = languageSwitchController.fallbackURL

      result mustEqual routes.ProfileSetupController.onPageLoad.url

    }

    "must return languageMap" in {

      val result = languageSwitchController.languageMap

      result mustEqual Map("en" -> Lang("en"), "cy" -> Lang("cy"))

    }
  }
}
