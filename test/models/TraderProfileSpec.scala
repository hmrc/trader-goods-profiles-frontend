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

import org.scalatest.Inside.inside
import base.TestConstants.{testEori, userAnswersId}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages._
import queries.TraderProfileQuery

class TraderProfileSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a TraderProfile when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberPage, "1")
            .success
            .value
            .set(HasNirmsPage, true)
            .success
            .value
            .set(NirmsNumberPage, "2")
            .success
            .value
            .set(HasNiphlPage, true)
            .success
            .value
            .set(NiphlNumberPage, "3")
            .success
            .value

        val result = TraderProfile.build(answers, testEori)

        result mustEqual Right(TraderProfile(testEori, "1", Some("2"), Some("3")))
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberPage, "1")
            .success
            .value
            .set(HasNirmsPage, false)
            .success
            .value
            .set(HasNiphlPage, false)
            .success
            .value

        val result = TraderProfile.build(answers, testEori)

        result mustEqual Right(TraderProfile(testEori, "1", None, None))
      }
    }

    "must return errors" - {

      "when all mandatory answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(UkimsNumberPage),
            PageMissing(HasNirmsPage),
            PageMissing(HasNiphlPage)
          )
        }
      }

      "when the user said they have a Nirms number but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberPage, "1")
            .success
            .value
            .set(HasNirmsPage, true)
            .success
            .value
            .set(HasNiphlPage, false)
            .success
            .value

        val result = TraderProfile.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NirmsNumberPage)
        }
      }

      "when the user said they have a Niphl number but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberPage, "1")
            .success
            .value
            .set(HasNirmsPage, false)
            .success
            .value
            .set(HasNiphlPage, true)
            .success
            .value

        val result = TraderProfile.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NiphlNumberPage)
        }
      }

      "when the user said they don't have optional data but it is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(UkimsNumberPage, "1")
            .success
            .value
            .set(HasNirmsPage, false)
            .success
            .value
            .set(NirmsNumberPage, "2")
            .success
            .value
            .set(HasNiphlPage, false)
            .success
            .value
            .set(NiphlNumberPage, "3")
            .success
            .value

        val result = TraderProfile.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(NirmsNumberPage),
            UnexpectedPage(NiphlNumberPage)
          )
        }
      }
    }
  }

  ".buildNirms" - {

    val traderProfile = TraderProfile(testEori, "1", None, None)

    "must return a TraderProfile when all nirms data is answered" - {

      "and nirms is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, true)
            .success
            .value
            .set(NirmsNumberUpdatePage, "2")
            .success
            .value

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        result mustEqual Right(TraderProfile(testEori, "1", Some("2"), None))
      }

      "and nirms is not present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(RemoveNirmsPage, true)
            .success
            .value

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        result mustEqual Right(traderProfile)
      }
    }

    "must return errors" - {

      "when mandatory answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNirmsUpdatePage)
          )
        }
      }

      "when the user said they have a Nirms number but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, true)
            .success
            .value

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NirmsNumberUpdatePage)
        }
      }

      "when the user said they don't have optional data but they haven't confirmed it" in {
        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(RemoveNirmsPage)
          )
        }
      }

      "when the user has confirmed deleting something they don't want to delete" in {
        val answers =
          UserAnswers(userAnswersId)
            .set(RemoveNirmsPage, false)
            .success
            .value
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(NirmsNumberUpdatePage, "123")
            .success
            .value

        val result = TraderProfile.buildNirms(answers, testEori, traderProfile)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(RemoveNirmsPage)
          )
        }
      }
    }
  }

  ".validateHasNirms" - {

    "must validate Nirms" - {

      "user has Nirms and changes answer to No" in {
        val userProfile = TraderProfile(testEori, "1", Some("nirms"), None)

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(RemoveNirmsPage, true)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustEqual Right(true)
      }

      "user does not have Nirms and does not change answer to Yes" in {
        val userProfile = TraderProfile(testEori, "1", None, None)

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustEqual Right(false)
      }

      "when user answered Yes" in {

        val userProfile = TraderProfile(testEori, "1", Some("nirms"), None)

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, true)
            .success
            .value
            .set(RemoveNirmsPage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustEqual Right(false)

      }

      "when user answered No but No to remove Nirms question" in {

        val userProfile = TraderProfile(testEori, "1", Some("nirms"), None)

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(RemoveNirmsPage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustEqual Right(false)

      }
    }

    "must return errors" - {

      "when user does not have answers" in {

        def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)
        val result                        = TraderProfile.validateHasNirms(emptyUserAnswers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNirmsUpdatePage)
          )
        }
      }

      "when TraderProfileQuery is not set" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, false)
            .success
            .value
            .set(RemoveNirmsPage, false)
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(TraderProfileQuery)
          )
        }
      }
    }
  }

  ".validateNiphlsUpdate" - {

    val userProfile = TraderProfile(testEori, "1", None, Some("niphls"))

    "must validate Niphls" - {

      "user has Niphls and changes answer to No" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(RemoveNiphlPage, true)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        result mustEqual Right(None)
      }

      "user does not have Niphls and does not change answer to Yes" in {
        val userProfile = TraderProfile(testEori, "1", None, None)

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        result mustEqual Right(None)
      }
    }

    "must return errors" - {

      "when user does not have answers" in {

        def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)
        val result                        = TraderProfile.validateNiphlsUpdate(emptyUserAnswers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNiphlUpdatePage)
          )
        }
      }

      "when user answered Yes" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, true)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(NiphlNumberUpdatePage)
          )
        }
      }

      "when user answered No but No to remove Niphls question" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(RemoveNiphlPage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(RemoveNiphlPage)
          )
        }
      }

      "when TraderProfileQuery is not set" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(RemoveNiphlPage, false)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(TraderProfileQuery)
          )
        }
      }

      "when user answered No and RemoveNiphlPage is not set" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasNiphlUpdatePage, false)
            .success
            .value
            .set(TraderProfileQuery, userProfile)
            .success
            .value

        val result = TraderProfile.validateNiphlsUpdate(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(RemoveNiphlPage)
          )
        }
      }

    }
  }

  ".validateUkimsNumber" - {

    "must validate Ukims Number" in {

      val answers =
        UserAnswers(userAnswersId)
          .set(UkimsNumberUpdatePage, "newUkims")
          .success
          .value

      val result = TraderProfile.validateUkimsNumber(answers)

      result mustEqual Right("newUkims")

    }

    "must return errors" - {

      "when user does not have answers" in {

        def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)
        val result                        = TraderProfile.validateUkimsNumber(emptyUserAnswers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(UkimsNumberUpdatePage)
          )
        }
      }
    }
  }
}
