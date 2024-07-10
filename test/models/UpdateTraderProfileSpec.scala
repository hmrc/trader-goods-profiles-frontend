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

import base.TestConstants.{testEori, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._

class UpdateTraderProfileSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return an UpdateTraderProfile when all data is valid" - {

      "and all ukims data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberUpdatePage, "1")
            .success
            .value

        val result = UpdateTraderProfile.buildUkimsNumber(answers, testEori)

        result mustEqual Right(
          UpdateTraderProfile(testEori, ukimsNumber = Some("1"))
        )
      }

      "and all niphl data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(NiphlNumberUpdatePage, "1")
            .success
            .value
            .set(HasNiphlUpdatePage, true)
            .success
            .value

        val result = UpdateTraderProfile.buildNiphlNumber(answers, testEori)

        result mustEqual Right(
          UpdateTraderProfile(testEori, niphlNumber = Some("1"))
        )
      }

      "and all nirms data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(NirmsNumberUpdatePage, "1")
            .success
            .value
            .set(HasNirmsUpdatePage, true)
            .success
            .value

        val result = UpdateTraderProfile.buildNirmsNumber(answers, testEori)

        result mustEqual Right(
          UpdateTraderProfile(testEori, nirmsNumber = Some("1"))
        )
      }

    }

    "must return errors" - {

      "when ukimsNumber is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateTraderProfile.buildUkimsNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(UkimsNumberUpdatePage)
        }
      }

      "when niphlNumber is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, true)
          .success
          .value

        val result = UpdateTraderProfile.buildNiphlNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NiphlNumberUpdatePage)
        }
      }

      "when niphlNumber is not required and is present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, false)
          .success
          .value
          .set(NiphlNumberUpdatePage, "1")
          .success
          .value

        val result = UpdateTraderProfile.buildNiphlNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(NiphlNumberUpdatePage)
        }
      }

      "when hasNiphlNumber is required and missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(NiphlNumberUpdatePage, "1")
          .success
          .value

        val result = UpdateTraderProfile.buildNiphlNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(HasNiphlUpdatePage)
        }
      }

      "when nirmsNumber is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, true)
          .success
          .value

        val result = UpdateTraderProfile.buildNirmsNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NirmsNumberUpdatePage)
        }
      }

      "when nirmsNumber is not required and is present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, false)
          .success
          .value
          .set(NirmsNumberUpdatePage, "1")
          .success
          .value

        val result = UpdateTraderProfile.buildNirmsNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(NirmsNumberUpdatePage)
        }
      }

      "when hasNirmsNumber is required and missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(NirmsNumberUpdatePage, "1")
          .success
          .value

        val result = UpdateTraderProfile.buildNirmsNumber(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(HasNirmsUpdatePage)
        }
      }
    }
  }
}
