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

class CommodityCodeFormProviderSpec extends StringFieldBehaviours {

  private val formProvider       = new CommodityCodeFormProvider()
  private val form: Form[String] = formProvider()
  private val requiredKey        = "commodityCode.error.required"
  private val invalidKey         = "commodityCode.error.invalidFormat"

  val validCommodityCodeGenerator: Gen[String] = {
    val validLengthsGen = Gen.oneOf(6, 8, 10)
    validLengthsGen.flatMap(length => Gen.listOfN(length, Gen.numChar).map(_.mkString))
  }

  val invalidCommodityCodeGenerator: Gen[String] = {
    val invalidLengthGen = Gen.choose(1, 12).suchThat(len => len != 6 && len != 8 && len != 10)
    val invalidCharsGen  = Gen.alphaNumStr.suchThat(_.exists(!_.isDigit))

    Gen.oneOf(
      invalidLengthGen.flatMap(length => Gen.listOfN(length, Gen.numChar).map(_.mkString)),
      Gen.oneOf(6, 8, 10).flatMap(length => Gen.listOfN(length, invalidCharsGen).map(_.mkString)),
      invalidLengthGen.flatMap(length => Gen.listOfN(length, Gen.alphaNumChar).map(_.mkString))
    )
  }

  ".commodityCode" - {

    val fieldName = "commodityCode"

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    behave like fieldThatBindsValidData(form, fieldName, validCommodityCodeGenerator)

    behave like fieldThatErrorsOnInvalidData(
      form,
      fieldName,
      invalidCommodityCodeGenerator,
      FormError(fieldName, invalidKey)
    )
  }

}
