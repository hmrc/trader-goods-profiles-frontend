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
import generators.NiphlsNumberGenerator
import play.api.data.FormError

class NiphlsNumberFormProviderSpec extends StringFieldBehaviours with NiphlsNumberGenerator {
  private val formProvider = new NiphlsNumberFormProvider()
  private val form         = formProvider()
  private val wrongFormat  = "niphlsNumber.error.wrongFormat"
  private val requiredKey  = "niphlsNumber.error.notSupplied"

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "errors with" - {
      "less than 4 characters" - {
        behave like fieldThatErrorsOnInvalidData(form, fieldName, stringsWithMaxLength(3), wrongFormat)
      }

      "more than 7 characters" - {
        behave like fieldThatErrorsOnInvalidData(form, fieldName, stringsLongerThan(7), wrongFormat)
      }

      "one letter and 4 numbers" - {
        behave like fieldThatErrorsOnInvalidData(form, fieldName, niphlsAlphaNumericGenerator(1, 4), wrongFormat)
      }

      "one letter and 6 numbers" - {
        behave like fieldThatErrorsOnInvalidData(form, fieldName, niphlsAlphaNumericGenerator(1, 6), wrongFormat)
      }

      "three letters and 5 numbers" - {
        behave like fieldThatErrorsOnInvalidData(form, fieldName, niphlsAlphaNumericGenerator(3, 5), wrongFormat)
      }

    }

    "accepts" - {
      "4 to 6 digits" - {
        behave like fieldThatBindsValidData(form, fieldName, niphlsNumericGenerator(1000, 999999))
      }

      "one letter and 5 numbers" - {
        behave like fieldThatBindsValidData(form, fieldName, niphlsAlphaNumericGenerator(1, 5))
      }

      "two letters and 5 numbers" - {
        behave like fieldThatBindsValidData(form, fieldName, niphlsAlphaNumericGenerator(2, 5))
      }
    }

  }

}
