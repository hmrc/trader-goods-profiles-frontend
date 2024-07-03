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

import base.TestConstants.{testEori, testRecordId, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._

class AdviceRequestSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a AdviceRequest when all mandatory questions are answered" - {

      val answers =
        UserAnswers(userAnswersId)
          .set(NamePage(testRecordId), "1")
          .success
          .value
          .set(EmailPage(testRecordId), "2")
          .success
          .value

      val result = AdviceRequest.build(answers, testEori, testRecordId)

      result mustEqual Right(AdviceRequest(testEori, "1", testEori, testRecordId, "2"))
    }

    "must return errors" - {

      "when all answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = AdviceRequest.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(NamePage(testRecordId)),
            PageMissing(EmailPage(testRecordId))
          )
        }
      }
    }
  }
}
