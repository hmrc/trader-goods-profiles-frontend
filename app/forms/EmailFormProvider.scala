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
import models.StringFieldRegex
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}
import org.apache.commons.validator.routines.EmailValidator
import utils.Constants
class EmailFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("email.error.required")
        .transform(removeWhitespace, identity[String])
        .verifying(emailConstraint)
    )

  private def emailConstraint: Constraint[String] = {
    val maxLengthConstraint = maxLength(Constants.maximumEmailLength, "email.error.length")
    val regexConstraint     = regexp(StringFieldRegex.emailRegex, "email.error.invalidFormat")
    val emailValidator      = EmailValidator.getInstance(true)

    Constraint { email =>
      maxLengthConstraint(email) match {
        case Valid            =>
          regexConstraint(email) match {
            case Valid if emailValidator.isValid(email) => Valid
            case Valid                                  => Invalid("email.error.invalidFormat") // Returns if the email fails the EmailValidator check
            case invalid: Invalid                       => invalid // Returns if the email fails the regex pattern check
          }
        case invalid: Invalid => invalid // Returns if the email exceeds the maximum length
      }
    }
  }
}
