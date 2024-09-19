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

package forms.mappings.helpers

import forms.mappings.helpers.FormatAnswers.{addHyphensToNirms, removeWhitespace, toUppercaseAndRemoveSpacesAndHyphens, trimAndCompressSpaces}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class FormatAnswersSpec extends AnyFreeSpec with Matchers {

  "removeWhitespace" - {
    "must remove whitespace" in {
      removeWhitespace("T E S T") mustBe "TEST"
    }
  }

  "toUppercaseAndRemoveSpacesAndHyphens" - {
    "must make uppercase and remove spaces and hyphens" in {
      toUppercaseAndRemoveSpacesAndHyphens("t e s-t") mustBe "TEST"
    }
  }

  "trimAndCompressSpaces" - {
    "must trim and compress spaces" in {
      trimAndCompressSpaces("     t      e s       t") mustBe "t e s t"
    }
  }

  "addHyphensToNirms" - {
    "must add hyphens to nirms number" in {
      addHyphensToNirms("RMSGB123456") mustBe "RMS-GB-123456"
    }
  }
}
