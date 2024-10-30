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

import javax.inject.Inject
import forms.mappings.Mappings
import forms.mappings.helpers.FormatAnswers.removeWhitespace
import forms.mappings.helpers.StopOnFirstFail
import models.StringFieldRegex
import play.api.data.Form
import utils.Constants

class EmailFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("email.error.required")
        .transform(removeWhitespace, identity[String])
        .verifying(
          StopOnFirstFail[String](
            maxLength(Constants.maximumEmailLength, "email.error.length"),
            regexp(StringFieldRegex.emailRegex, "email.error.invalidFormat"),
            email("email.error.invalidFormat")
          )
        )
    )
}
