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

package forms.profile.nirms

import forms.mappings.Mappings
import forms.mappings.helpers.FormatAnswers.{addHyphensToNirms, toUppercaseAndRemoveSpacesAndHyphens}
import models.StringFieldRegex
import play.api.data.Form

import javax.inject.Inject

class NirmsNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("nirmsNumber.error.required")
        .transform(toUppercaseAndRemoveSpacesAndHyphens, identity[String])
        .verifying(regexp(StringFieldRegex.nirmsRegex, "nirmsNumber.error.invalidFormat"))
        .transform(addHyphensToNirms, identity[String])
    )
}