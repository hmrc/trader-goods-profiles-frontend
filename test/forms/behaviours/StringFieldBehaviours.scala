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

package forms.behaviours

import org.scalacheck.Gen
import play.api.data.{Form, FormError}

trait StringFieldBehaviours extends FieldBehaviours {

  def fieldWithMaxLength(
    form: Form[_],
    fieldName: String,
    maxLength: Int,
    lengthError: FormError,
    generateString: Int => Gen[String] = stringsLongerThan
  ): Unit =
    s"not bind strings longer than $maxLength characters" in {

      forAll(generateString(maxLength) -> "longString") { string =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        result.errors.find(_ == lengthError) mustBe defined
      }
    }

  def fieldThatErrorsOnInvalidData(
    form: Form[_],
    fieldName: String,
    invalidDataGenerator: Gen[String],
    invalidError: String
  ): Unit =
    "error on invalid data" in {

      forAll(invalidDataGenerator -> "invalidDataItem") { (dataItem: String) =>
        val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
        result.errors.size mustBe 1
        result.errors.find(_.message == invalidError) mustBe defined
      }
    }
}
