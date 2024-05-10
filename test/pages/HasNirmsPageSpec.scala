package pages

import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HasNirmsPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {
  "clean up" - {

    "removes NIRMS number when the answer is No" in {

      val userAnswers = UserAnswers("id").set(NirmsNumberPage, "1234").success.value

      val result = userAnswers.set(HasNirmsPage, false).success.value

      result.isDefined(NirmsNumberPage) mustBe false

      result.get(HasNirmsPage).value mustBe false

    }

    "does not remove NIRMS number when the answer is Yes" in {

      val userAnswers = UserAnswers("id").set(NirmsNumberPage, "1234").success.value

      val result = userAnswers.set(HasNirmsPage, true).success.value

      result.isDefined(NirmsNumberPage) mustBe true

      result.get(HasNirmsPage).value mustBe true
    }
  }
}
