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

import forms.behaviours.IntFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class LongerCommodityCodeFormProviderSpec extends IntFieldBehaviours {

  private val formProvider = new LongerCommodityCodeFormProvider()
  private val form         = formProvider()
  private val requiredKey  = "longerCommodityCode.error.required"
  private val wrongFormat  = "longerCommodityCode.error.invalidFormat"

  private val fieldName = "value"

  val validAdditionalNumbersGenerator: Gen[String] = {
    val validLengthsGen = Gen.oneOf(2, 4)
    validLengthsGen.flatMap(length => Gen.listOfN(length, Gen.numChar).map(_.mkString))
  }

  val invalidAdditionalNumbersGenerator: Gen[String] = {
    val validLengthsGen = Gen.oneOf(1, 3)
    validLengthsGen.flatMap(length => Gen.listOfN(length, Gen.numChar).map(_.mkString))
  }

  ".value" - {

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    behave like fieldThatBindsValidData(form, fieldName, validAdditionalNumbersGenerator)

    "errors with" - {
      "1 or 3 numbers" - {
        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          invalidAdditionalNumbersGenerator,
          FormError(fieldName, wrongFormat)
        )
      }

      "more than 4 numbers" - {
        behave like fieldThatErrorsOnInvalidData(
          form,
          fieldName,
          stringsLongerThan(4),
          FormError(fieldName, wrongFormat)
        )
      }
    }

  }
}
