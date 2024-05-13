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
import models.helper.RemoveWhitespace.removeWhitespace
import play.api.data.Form

class NiphlNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("niphlNumber.error.required")
        .transform(removeWhitespace, identity[String])
        .verifying(regexp("^([0-9]{4,6}|[a-zA-Z]{1,2}[0-9]{5})$", "niphlNumber.error.invalidFormat"))
    )
}
