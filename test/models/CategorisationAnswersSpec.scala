package models

import base.TestConstants.{testEori, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.{HasNiphlPage, HasNirmsPage, HasSupplementaryUnitPage, NiphlNumberPage, NirmsNumberPage, SupplementaryUnitPage, UkimsNumberPage}

class CategorisationAnswersSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a CategorisationAnswer when all mandatory questions are answered" - {

      "and supplementary unit was not asked of them" in {

        val answers =
          UserAnswers(userAnswersId)

        val result = CategorisationAnswers.build(answers)

        result mustEqual Right(CategorisationAnswers(Seq.empty, None))
      }

      "and supplementary unit was asked for and the answer was no" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, false)
            .success
            .value

        val result = CategorisationAnswers.build(answers)

        result mustEqual Right(CategorisationAnswers(Seq.empty, None))
      }

      "and supplementary unit was asked for and the answer was yes and it was supplied" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value
            .set(SupplementaryUnitPage, 42)
            .success
            .value

        val result = CategorisationAnswers.build(answers)

        result mustEqual Right(CategorisationAnswers(Seq.empty, Some(42)))
      }

    }

    "must return errors" - {

//        "when all mandatory answers are missing" in {
//
//          val answers = UserAnswers(userAnswersId)
//
//          val result = TraderProfile.build(answers, testEori)
//
//          inside(result) { case Left(errors) =>
//            errors.toChain.toList must contain theSameElementsAs Seq(
//              PageMissing(UkimsNumberPage),
//              PageMissing(HasNirmsPage),
//              PageMissing(HasNiphlPage)
//            )
//          }
//        }

      "when the user said they have a supplementary unit but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(HasSupplementaryUnitPage, true)
            .success
            .value

        val result = CategorisationAnswers.build(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(SupplementaryUnitPage)
        }
      }

      "when the user has a supplementary unit without being asked about it " in {

        val answers =
          UserAnswers(userAnswersId)
            .set(SupplementaryUnitPage, 42)
            .success
            .value

        val result = CategorisationAnswers.build(answers)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(SupplementaryUnitPage)
        }
      }

    }
  }
}
