package pages

import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HasNiphlPageSpec extends  AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "removes NIPHL number when the answer is No" in {

      val userAnswers = UserAnswers("id").set(NiphlNumberPage, "1234").success.value

      val result = userAnswers.set(HasNiphlPage, false).success.value

      result.isDefined(NiphlNumberPage) mustBe false

      result.get(HasNiphlPage).value mustBe false
    }

    "does not removes NIPHL number when the answer is Yes" in {

      val userAnswers = UserAnswers("id").set(NiphlNumberPage, "1234").success.value

      val result = userAnswers.set(HasNiphlPage, true).success.value

      result.isDefined(NiphlNumberPage) mustBe true

      result.get(HasNiphlPage).value mustBe true
    }
  }

}
