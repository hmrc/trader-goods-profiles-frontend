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
import pages.profile.niphl._
import pages.profile.nirms._
import pages.profile.ukims.{UkimsNumberPage, UkimsNumberUpdatePage}
import queries.TraderProfileQuery

class TraderProfileSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  val traderProfile: TraderProfile =
    TraderProfile("actorId", "ukims", Some("nirmsNumber"), Some("niphlNumber"), eoriChanged = false)

  ".build" - {

    "must return a TraderProfile when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers = UserAnswers(userAnswersId)
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

        result mustBe Right(TraderProfile(testEori, "1", Some("2"), Some("3"), eoriChanged = false))
      }

      "and all optional data is missing" in {

        val answers = UserAnswers(userAnswersId)
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

        result mustBe Right(TraderProfile(testEori, "1", None, None, eoriChanged = false))
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
        val answers = UserAnswers(userAnswersId)
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
        val answers = UserAnswers(userAnswersId)
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
        val answers = UserAnswers(userAnswersId)
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

  "validateHasNirms" - {

    "must return a TraderProfile when all nirms data is answered" - {

      "and nirms is not present" in {
        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, false)
          .success
          .value
          .set(RemoveNirmsPage, true)
          .success
          .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustBe Right(None)
      }

      "except RemoveNirms and TraderProfileQuery has no nirms number" in {
        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, false)
          .success
          .value
          .set(TraderProfileQuery, traderProfile.copy(nirmsNumber = None))
          .success
          .value

        val result = TraderProfile.validateHasNirms(answers)

        result mustBe Right(None)
      }
    }

    "must return errors" - {

      "and nirms is present" in {
        val answers =
          UserAnswers(userAnswersId)
            .set(HasNirmsUpdatePage, true)
            .success
            .value
            .set(NirmsNumberUpdatePage, "2")
            .success
            .value

        val result = TraderProfile.validateHasNirms(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            IncorrectlyAnsweredPage(HasNirmsUpdatePage)
          )
        }
      }

      "when mandatory answers are missing" in {
        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.validateHasNirms(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNirmsUpdatePage)
          )
        }
      }

      "when the user said they don't have optional data but they haven't confirmed it" in {
        val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, false).success.value

        val result = TraderProfile.validateHasNirms(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(RemoveNirmsPage)
          )
        }
      }

      "when the user has confirmed deleting something they don't want to delete" in {
        val answers = UserAnswers(userAnswersId)
          .set(RemoveNirmsPage, false)
          .success
          .value
          .set(HasNirmsUpdatePage, false)
          .success
          .value
          .set(NirmsNumberUpdatePage, "123")
          .success
          .value

        val result = TraderProfile.validateHasNirms(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(RemoveNirmsPage)
          )
        }
      }
    }
  }

  "validateNirmsNumber" - {

    "must return a TraderProfile when all nirms data is answered" - {

      "and nirms is present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, true)
          .success
          .value
          .set(NirmsNumberUpdatePage, "2")
          .success
          .value

        val result = TraderProfile.validateNirmsNumber(answers)

        result mustBe Right(Some("2"))
      }

    }

    "must return errors" - {

      "and nirms is not present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNirmsUpdatePage, false)
          .success
          .value
          .set(RemoveNirmsPage, true)
          .success
          .value

        val result = TraderProfile.validateNirmsNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            IncorrectlyAnsweredPage(HasNirmsUpdatePage)
          )
        }
      }

      "when mandatory answers are missing" in {
        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.validateNirmsNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNirmsUpdatePage)
          )
        }
      }

      "when the user said they have a Nirms number but it is missing" in {
        val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value

        val result = TraderProfile.validateNirmsNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NirmsNumberUpdatePage)
        }
      }
    }
  }

  "validateHasNiphl" - {

    "must return Niphl number when all niphl data is answered" - {

      "and niphl is not present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, false)
          .success
          .value
          .set(RemoveNiphlPage, true)
          .success
          .value

        val result = TraderProfile.validateHasNiphl(answers)

        result mustBe Right(None)
      }

      "except RemoveNiphl and TraderProfileQuery has no niphl number" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, false)
          .success
          .value
          .set(TraderProfileQuery, traderProfile.copy(niphlNumber = None))
          .success
          .value

        val result = TraderProfile.validateHasNiphl(answers)

        result mustBe Right(None)
      }
    }

    "must return errors" - {

      "and niphl is present" in {
        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, true)
          .success
          .value
          .set(NiphlNumberUpdatePage, "2")
          .success
          .value

        val result = TraderProfile.validateHasNiphl(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            IncorrectlyAnsweredPage(HasNiphlUpdatePage)
          )
        }
      }

      "when mandatory answers are missing" in {
        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.validateHasNiphl(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNiphlUpdatePage)
          )
        }
      }

      "when the user said they don't have optional data but they haven't confirmed it" in {
        val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, false).success.value

        val result = TraderProfile.validateHasNiphl(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(RemoveNiphlPage)
          )
        }
      }

      "when the user has confirmed deleting something they don't want to delete" in {
        val answers = UserAnswers(userAnswersId)
          .set(RemoveNiphlPage, false)
          .success
          .value
          .set(HasNiphlUpdatePage, false)
          .success
          .value
          .set(NiphlNumberUpdatePage, "123")
          .success
          .value

        val result = TraderProfile.validateHasNiphl(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(RemoveNiphlPage)
          )
        }
      }
    }
  }

  "validateNiphlNumber" - {
    "must return a Niphl Number when all niphl data is answered" - {
      "and niphl is present" in {
        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, true)
          .success
          .value
          .set(NiphlNumberUpdatePage, "2")
          .success
          .value

        val result = TraderProfile.validateNiphlNumber(answers)

        result mustBe Right(Some("2"))
      }

    }

    "must return errors" - {

      "and niphl is not present" in {
        val answers = UserAnswers(userAnswersId)
          .set(HasNiphlUpdatePage, false)
          .success
          .value
          .set(RemoveNiphlPage, true)
          .success
          .value

        val result = TraderProfile.validateNiphlNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            IncorrectlyAnsweredPage(HasNiphlUpdatePage)
          )
        }
      }

      "when mandatory answers are missing" in {
        val answers = UserAnswers(userAnswersId)

        val result = TraderProfile.validateNiphlNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(HasNiphlUpdatePage)
          )
        }
      }

      "when the user said they have a Niphl number but it is missing" in {
        val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value

        val result = TraderProfile.validateNiphlNumber(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(NiphlNumberUpdatePage)
        }
      }
    }
  }

  "validateUkimsNumber" - {
    "must validate Ukims Number" in {
      val answers = UserAnswers(userAnswersId).set(UkimsNumberUpdatePage, "newUkims").success.value

      val result = TraderProfile.validateUkimsNumber(answers)

      result mustBe Right("newUkims")

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
