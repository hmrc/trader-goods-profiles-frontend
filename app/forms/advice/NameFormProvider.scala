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

package forms.advice

import forms.mappings.Mappings
import forms.mappings.helpers.FormatAnswers.trimAndCompressSpaces
import play.api.data.Form
import utils.Constants.maxNameLength

import javax.inject.Inject

class NameFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("name.error.required")
        .transform(trimAndCompressSpaces, identity[String])
        .verifying(maxLength(maxNameLength, "name.error.length"))
    )
}