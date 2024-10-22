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
import play.api.libs.json.{JsValue, Json}

class AssessmentFormProviderSpec extends StringFieldBehaviours with ScalaCheckPropertyChecks {

  private val form = new AssessmentFormProvider()(2)

  ".value" - {

    val fieldName       = "value"
    val maxCharsForJson = 64

    "must bind when the user checks none of the above" in {
      val jsonData: JsValue = Json.obj("value" -> Json.arr("none"))
      val result            = form.bind(jsonData, maxCharsForJson)
      result.errors mustBe empty
      result.get mustEqual AssessmentAnswer.NoExemption

      withClue("and store this single selection in an array") {
        val filledForm = form.fill(result.get)
        filledForm("value[0]").value mustBe Some("none")
        filledForm("value[1]").value mustBe None
      }
    }

    "must bind when the user checks some codes" in {
      val jsonData: JsValue = Json.obj("value" -> Json.arr("Y903", "Y508"))
      val result            = form.bind(jsonData, maxCharsForJson)
      result.errors mustBe empty
      result.get mustEqual AssessmentAnswer.Exemption(Seq("Y903", "Y508"))

      withClue("and store these selections in an array") {
        val filledForm = form.fill(result.get)
        filledForm("value[0]").value mustBe Some("Y903")
        filledForm("value[1]").value mustBe Some("Y508")
        filledForm("value[2]").value mustBe None
      }
    }

    "must not bind invalid value" in {
      val result = form.bind(Map("value" -> ""))
      result.errors must contain only FormError(fieldName, "assessment.error.required")
    }

    "unanswered should be considered an empty array" in {
      val filledForm = form.fill(AssessmentAnswer.NotAnsweredYet)
      filledForm("value[0]").value mustBe None
    }

  }
}
