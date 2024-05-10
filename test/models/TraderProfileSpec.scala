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
          UkimsNumberPage,
          HasNirmsPage,
          HasNiphlPage
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
        errors.value.toChain.toList must contain only NirmsNumberPage
      }

      "when the user said they have a Niphl number but it is missing" in {

        val answers =
          UserAnswers("id")
            .set(UkimsNumberPage, "1").success.value
            .set(HasNirmsPage, false).success.value
            .set(HasNiphlPage, true).success.value

        val (errors, data) = TraderProfile.build(answers).pad

        data must not be defined
        errors.value.toChain.toList must contain only NiphlNumberPage
      }
    }
  }
}
