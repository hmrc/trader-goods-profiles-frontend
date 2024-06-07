package pages

import base.TestConstants.userAnswersId
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HasSupplementaryUnitPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "removes SupplementaryUnit when the answer is No" in {

      val userAnswers = UserAnswers(userAnswersId).set(SupplementaryUnitPage, 42).success.value

      val result = userAnswers.set(HasSupplementaryUnitPage, false).success.value

      result.isDefined(SupplementaryUnitPage) mustBe false

      result.get(HasSupplementaryUnitPage).value mustBe false

    }

    "does not remove SupplementaryUnit when the answer is Yes" in {

      val userAnswers = UserAnswers(userAnswersId).set(SupplementaryUnitPage, 42).success.value

      val result = userAnswers.set(HasSupplementaryUnitPage, true).success.value

      result.isDefined(SupplementaryUnitPage) mustBe true

      result.get(HasSupplementaryUnitPage).value mustBe true
    }
  }
}
