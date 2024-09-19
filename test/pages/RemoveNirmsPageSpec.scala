package pages

import base.TestConstants.userAnswersId
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RemoveNirmsPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "clean up" - {

    "ensures HasNirmsUpdate is true when answer for RemoveNirms is false and NirmsNumberUpdatePage is present" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(NirmsNumberUpdatePage, "nirms-example")
        .success
        .value

      val result = userAnswers
        .set(RemoveNirmsPage, false)
        .success
        .value

      result.get(HasNirmsUpdatePage) mustBe Some(true)
    }

    "ensures HasNirmsUpdate is false when answer for RemoveNirms is true and NirmsNumberUpdatePage is present" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(NirmsNumberUpdatePage, "nirms-example")
        .success
        .value

      val result = userAnswers
        .set(RemoveNirmsPage, true)
        .success
        .value

      result.get(HasNirmsUpdatePage) mustBe Some(false)
    }

    "does not change HasNirmsUpdate when NirmsNumberUpdatePage is not present" in {
      val userAnswers = UserAnswers(userAnswersId)

      val result = userAnswers
        .set(RemoveNirmsPage, false)
        .success
        .value

      result.get(HasNirmsUpdatePage) mustBe None
    }

  }
}