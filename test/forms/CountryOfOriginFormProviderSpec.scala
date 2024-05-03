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

class CountryOfOriginFormProviderSpec extends StringFieldBehaviours {

  private val formProvider        = new CountryOfOriginFormProvider()
  private val form: Form[String]  = formProvider()
  private val requiredErrorKey    = "countryOfOrigin.error.required"
  private val lettersOnlyErrorKey = "countryOfOrigin.error.lettersOnly"
  private val lengthErrorKey      = "countryOfOrigin.error.length"
  private val fieldName           = "countryOfOrigin"

  ".countryOfOrigin" - {

    "valid country code" - {
      val validCountryCodeGenerator: Gen[String] = for {
        chars     <- Gen.listOfN(2, Gen.alphaChar)
        addSpaces <- Gen.oneOf(true, false)
      } yield if (addSpaces) chars.mkString(" ") else chars.mkString

      behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredErrorKey))
      behave like fieldThatBindsValidData(form, fieldName, validCountryCodeGenerator)
    }

    "invalid country code" - {

      "country code with invalid length" - {
        val invalidLengthCountryCodeGenerator: Gen[String] = for {
          chars     <- Gen.listOfN(3, Gen.alphaChar)
          addSpaces <- Gen.oneOf(true, false)
        } yield if (addSpaces) chars.mkString(" ") else chars.mkString

        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidLengthCountryCodeGenerator,
          FormError(fieldName, lengthErrorKey)
        )
      }

      "country code containing non-alphabet characters" - {
        val invalidCountryCodeWithNumbersGenerator: Gen[String] = for {
          chars     <- Gen.listOfN(2, Gen.numChar)
          addSpaces <- Gen.oneOf(true, false)
        } yield if (addSpaces) chars.mkString(" ") else chars.mkString

        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidCountryCodeWithNumbersGenerator,
          FormError(fieldName, lettersOnlyErrorKey)
        )
      }
    }

  }
}
