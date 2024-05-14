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

package models

import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages._

class TraderProfileSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a TraderProfile when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, true).success.value
            .set(NirmsNumberPage, "2").success.value
            .set(HasNiphlPage, true).success.value
            .set(NiphlNumberPage, "3").success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data.value mustEqual TraderProfile("1", Some("2"), Some("3"))
        errors must not be defined
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, false).success.value
            .set(HasNiphlPage, false).success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data.value mustEqual TraderProfile("1", None, None)
        errors must not be defined
      }
    }

    "must return errors" - {

      "when all mandatory answers are missing" in {

        val answers = UserAnswers("id")

        val (errors, data) = TraderProfile.build(answers).pad

        data must not be defined
        errors.value.toChain.toList must contain theSameElementsAs Seq(
          PageMissing(UkimsNumberPage),
          PageMissing(HasNirmsPage),
          PageMissing(HasNiphlPage)
        )
      }

      "when the user said they have a Nirms number but it is missing" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, true).success.value
            .set(HasNiphlPage, false).success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data must not be defined
        errors.value.toChain.toList must contain only PageMissing(NirmsNumberPage)
      }

      "when the user said they have a Niphl number but it is missing" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, false).success.value
            .set(HasNiphlPage, true).success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data must not be defined
        errors.value.toChain.toList must contain only PageMissing(NiphlNumberPage)
      }

      "when the user said they don't have optional data but it is present" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, false).success.value
            .set(NirmsNumberPage, "2").success.value
            .set(HasNiphlPage, false).success.value
            .set(NiphlNumberPage, "3").success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data must not be defined
        errors.value.toChain.toList must contain theSameElementsAs Seq(
          UnexpectedPage(NirmsNumberPage),
          UnexpectedPage(NiphlNumberPage)
        )
      }
    }
  }
}
