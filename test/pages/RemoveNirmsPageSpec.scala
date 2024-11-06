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

package pages

import base.TestConstants.userAnswersId
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.profile.{HasNirmsUpdatePage, NirmsNumberUpdatePage, RemoveNirmsPage}

class RemoveNirmsPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "ensures HasNirmsUpdate is true when answer for RemoveNirms is false and NirmsNumberUpdatePage is present" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(NirmsNumberUpdatePage, "nirms-example")
        .success
        .value

      val result = userAnswers
        .set(RemoveNirmsPage, false)
        .success
        .value

      result.get(HasNirmsUpdatePage) mustBe Some(true)
    }

    "ensures HasNirmsUpdate is false when answer for RemoveNirms is true and NirmsNumberUpdatePage is present" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(NirmsNumberUpdatePage, "nirms-example")
        .success
        .value

      val result = userAnswers
        .set(RemoveNirmsPage, true)
        .success
        .value

      result.get(HasNirmsUpdatePage) mustBe Some(false)
    }

  }
}
