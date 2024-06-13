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

import base.TestConstants.{testRecordId, userAnswersId}
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HasSupplementaryUnitPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "removes SupplementaryUnit when the answer is No" in {

      val userAnswers = UserAnswers(userAnswersId).set(SupplementaryUnitPage(testRecordId), 42.0).success.value

      val result = userAnswers.set(HasSupplementaryUnitPage(testRecordId), false).success.value

      result.isDefined(SupplementaryUnitPage(testRecordId)) mustBe false

      result.get(HasSupplementaryUnitPage(testRecordId)).value mustBe false

    }

    "removes SupplementaryUnit for a different record id" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(SupplementaryUnitPage("differentRecordId"), 42.0)
        .success
        .value
        .set(SupplementaryUnitPage(testRecordId), 43.0)
        .success
        .value

      val result = userAnswers.set(HasSupplementaryUnitPage(testRecordId), false).success.value

      result.isDefined(SupplementaryUnitPage(testRecordId)) mustBe false
      result.get(SupplementaryUnitPage("differentRecordId")).value mustBe 42.0

      result.get(HasSupplementaryUnitPage(testRecordId)).value mustBe false

    }

    "does not remove SupplementaryUnit when the answer is Yes" in {

      val userAnswers = UserAnswers(userAnswersId).set(SupplementaryUnitPage(testRecordId), 42.0).success.value

      val result = userAnswers.set(HasSupplementaryUnitPage(testRecordId), true).success.value

      result.isDefined(SupplementaryUnitPage(testRecordId)) mustBe true

      result.get(HasSupplementaryUnitPage(testRecordId)).value mustBe true
    }
  }
}
