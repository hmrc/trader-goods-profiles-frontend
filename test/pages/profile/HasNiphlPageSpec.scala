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

package pages.profile

import base.TestConstants.userAnswersId
import models.UserAnswers
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.profile.niphl.{HasNiphlPage, NiphlNumberPage}

class HasNiphlPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "removes NIPHL number when the answer is No" in {

      val userAnswers = UserAnswers(userAnswersId).set(NiphlNumberPage, "1234").success.value

      val result = userAnswers.set(HasNiphlPage, false).success.value

      result.isDefined(NiphlNumberPage) mustBe false

      result.get(HasNiphlPage).value mustBe false
    }

    "does not removes NIPHL number when the answer is Yes" in {

      val userAnswers = UserAnswers(userAnswersId).set(NiphlNumberPage, "1234").success.value

      val result = userAnswers.set(HasNiphlPage, true).success.value

      result.isDefined(NiphlNumberPage) mustBe true

      result.get(HasNiphlPage).value mustBe true
    }
  }
}
