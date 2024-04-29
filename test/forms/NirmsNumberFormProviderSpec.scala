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

class NirmsNumberFormProviderSpec extends StringFieldBehaviours {
  private val formProvider       = new NirmsNumberFormProvider()
  private val form: Form[String] = formProvider()
  private val requiredKey        = "nirmsNumber.error.required"
  private val invalidKey         = "nirmsNumber.error.invalidFormat"
  private val rmsRegex           = "RMS-?(GB|NI)-?[0-9]{6}"

  val nirmsNumberGenerator: Gen[String] = {
    val regionGen = Gen.oneOf("GB", "NI")
    val digitsGen = Gen.listOfN(6, Gen.numChar).map(_.mkString)
    val hyphenGen = Gen.oneOf("", "-")

    for {
      region  <- regionGen
      digits  <- digitsGen
      hyphen1 <- hyphenGen
      hyphen2 <- hyphenGen
    } yield s"RMS$hyphen1$region$hyphen2$digits"
  }

  val nonNirmsNumberGenerator: Gen[String] = {
    val invalidRegionGen = Gen.alphaStr.suchThat(s => s != "GB" && s != "NI" && s.nonEmpty)
    val invalidDigitsGen = for {
      length <- Gen.choose(1, 10)
      digits <- Gen.listOfN(length, Gen.oneOf(Gen.alphaChar, Gen.numChar))
    } yield digits.mkString

    Gen.oneOf(
      invalidRegionGen.map(region => s"RMS-$region-123456"),
      invalidDigitsGen.map(digits => s"RMS-GB-$digits")
    )
  }

  ".nirmsNumber" - {

    val fieldName = "nirmsNumber"

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    behave like fieldThatBindsValidData(form, fieldName, nirmsNumberGenerator)

    behave like fieldThatErrorsOnInvalidData(form, fieldName, nonNirmsNumberGenerator, FormError(fieldName, invalidKey))
  }
}
