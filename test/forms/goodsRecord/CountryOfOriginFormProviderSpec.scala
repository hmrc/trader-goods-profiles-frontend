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

package forms.goodsRecord

import forms.behaviours.StringFieldBehaviours
import models.Country
import play.api.data.FormError

class CountryOfOriginFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "countryOfOrigin.error.required"
  private val invalidKey  = "countryOfOrigin.error.invalid"
  private val countries   = Seq(Country("CN", "China"))
  private val form        = new CountryOfOriginFormProvider()(countries)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      "CN"
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldThatErrorsOnInvalidData(
      form,
      fieldName,
      "TEST",
      FormError(fieldName, invalidKey)
    )
  }
}
