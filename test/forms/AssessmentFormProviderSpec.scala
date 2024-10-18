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

package forms

import forms.behaviours.StringFieldBehaviours
import models.AssessmentAnswer
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.FormError

class AssessmentFormProviderSpec extends StringFieldBehaviours with ScalaCheckPropertyChecks {

  private val form2 = new AssessmentFormProvider()(1)

  ".value2" - {

    val fieldName = "value"

    "must bind `false`" in {

      val result = form2.bind(Map("value" -> "false"))
      result.errors mustBe empty
      result.get mustEqual AssessmentAnswer.NoExemption
    }

    "must bind `true`" in {
      val result = form2.bind(Map("value" -> "true"))
      result.errors mustBe empty
      result.get mustEqual AssessmentAnswer.Exemption(Seq("TEST_CODE"))
    }

    "must not bind invalid value" in {
      val result = form2.bind(Map("value" -> ""))
      result.errors must contain only FormError(fieldName, "assessment.error.required.onlyOne")
    }
  }
}
