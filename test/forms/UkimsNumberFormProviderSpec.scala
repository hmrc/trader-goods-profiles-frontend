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

class UkimsNumberFormProviderSpec extends StringFieldBehaviours {

  private val formProvider       = new UkimsNumberFormProvider()
  private val form: Form[String] = formProvider()
  private val requiredKey        = "ukimsNumber.error.required"
  private val invalidFormatKey   = "ukimsNumber.error.invalidFormat"
  private val fieldName          = "value"

  ".ukimsNumber" - {

    "valid UKIMS number" - {

      val ukimsNumberGenerator: Gen[String] = {
        val prefixGen      = Gen.oneOf("GB", "XI")
        val firstDigitsGen = Gen.listOfN(12, Gen.numChar).map(_.mkString)
        val lastDigitsGen  = Gen.listOfN(14, Gen.numChar).map(_.mkString)

        for {
          prefix      <- prefixGen
          firstDigits <- firstDigitsGen
          lastDigits  <- lastDigitsGen
        } yield s"$prefix UKIM$firstDigits$lastDigits"
      }

      behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

      behave like fieldThatBindsValidData(form, fieldName, ukimsNumberGenerator)

    }

    "valid UKIMS number with spaces" - {

      val ukimsNumberGenerator: Gen[String] = {
        val prefixGen      = Gen.oneOf("GB", "XI")
        val firstDigitsGen = Gen.listOfN(12, Gen.numChar).map(_.mkString)
        val lastDigitsGen  = Gen.listOfN(14, Gen.numChar).map(_.mkString)

        for {
          prefix      <- prefixGen
          firstDigits <- firstDigitsGen
          lastDigits  <- lastDigitsGen
        } yield s"$prefix UKIM $firstDigits $lastDigits"
      }

      behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

      behave like fieldThatBindsValidData(form, fieldName, ukimsNumberGenerator)

    }

    "invalid UKIMS number" - {

      "invalid UKIMS number with invalid length" - {

        val invalidUkimsNumberGeneratorWithInvalidLength: Gen[String] = {

          val prefixGen      = Gen.oneOf("GB", "XI")
          val firstDigitsGen = Gen.listOfN(12, Gen.numChar).map(_.mkString)
          val lastDigitsGen  = Gen.listOfN(15, Gen.numChar).map(_.mkString)

          for {
            prefix      <- prefixGen
            firstDigits <- firstDigitsGen
            lastDigits  <- lastDigitsGen
          } yield s"$prefix UKIM$firstDigits$lastDigits"
        }

        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidUkimsNumberGeneratorWithInvalidLength,
          FormError(fieldName, invalidFormatKey)
        )
      }

      "invalid UKIMS number with invalid prefix" - {

        val invalidUkimsNumberGeneratorWithInvalidPrefix: Gen[String] = {
          val prefixGen      = Gen.oneOf("AA", "BB")
          val firstDigitsGen = Gen.listOfN(12, Gen.numChar).map(_.mkString)
          val lastDigitsGen  = Gen.listOfN(14, Gen.numChar).map(_.mkString)

          for {
            prefix      <- prefixGen
            firstDigits <- firstDigitsGen
            lastDigits  <- lastDigitsGen
          } yield s"$prefix UKIM$firstDigits$lastDigits"
        }

        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidUkimsNumberGeneratorWithInvalidPrefix,
          FormError(fieldName, invalidFormatKey)
        )

      }

      "invalid UKIMS number with special characters" - {

        val invalidUkimsNumberGeneratorWithSpecialCharacters: Gen[String] = {
          val prefixGen      = Gen.oneOf("  ", "A-", "B_")
          val firstDigitsGen = Gen.listOfN(12, Gen.numChar).map(_.mkString)
          val lastDigitsGen  = Gen.listOfN(14, Gen.numChar).map(_.mkString)

          for {
            prefix      <- prefixGen
            firstDigits <- firstDigitsGen
            lastDigits  <- lastDigitsGen
          } yield s"$prefix UKIM$firstDigits$lastDigits"
        }

        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidUkimsNumberGeneratorWithSpecialCharacters,
          FormError(fieldName, invalidFormatKey)
        )

      }
    }
  }
}
