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
import play.api.data.FormError

class EmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey      = "email.error.required"
  val lengthKey        = "email.error.length"
  val invalidFormatKey = "email.error.invalidFormat"

  val validEmail           = "test@test.co.uk"
  val validEmailWithSpaces = "test @test.co.uk"
  val invalidEmail         = "test"
  val longValidEmail       =
    "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890@test.co.uk"

  val form = new EmailFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validEmail
    )

    "with spaces" - {

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        validEmailWithSpaces
      )
    }

    "too long" - {

      behave like fieldThatErrorsOnInvalidData(
        form,
        fieldName,
        longValidEmail,
        invalidError = FormError(fieldName, lengthKey)
      )
    }

    "invalid email" - {

      behave like fieldThatErrorsOnInvalidData(
        form,
        fieldName,
        invalidEmail,
        invalidError = FormError(fieldName, invalidFormatKey)
      )
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
