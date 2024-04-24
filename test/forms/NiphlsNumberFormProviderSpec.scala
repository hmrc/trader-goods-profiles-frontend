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
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class NiphlsNumberFormProviderSpec extends StringFieldBehaviours {
  private val formProvider = new NiphlsNumberFormProvider()
  private val form         = formProvider()
  private val wrongFormat  = "niphlsNumber.error.wrongFormat"
  private val requiredKey  = "niphlsNumber.error.notSupplied"

  ".value" - {

    val fieldName = "value"

    //TODO
    //behave like fieldWithMaxLength(form, fieldName, 7, lengthError = FormError(fieldName, wrongFormat))

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "anything with less than 4 characters errors" - {
      behave like fieldThatErrorsOnInvalidData(form, fieldName, stringsWithMaxLength(3), FormError(fieldName, wrongFormat))
    }

    "anything with more than 7 characters errors" - {
      behave like fieldThatErrorsOnInvalidData(form, fieldName, stringsLongerThan(7), FormError(fieldName, wrongFormat))
    }

    // 4 to 6 numbers - \/
    //start with one letter, then 5 numbers
    //start with 2 letters, then 5 numbers
    "accept 4 to 6 digits" - {
      behave like fieldThatBindsValidData(form, fieldName, intsInRange(1000, 999999))
    }


  }
}
