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

import forms.mappings.Mappings
import models.AssessmentAnswer
import play.api.data.Form
import javax.inject.Inject
import play.api.data.Forms.seq

class AssessmentFormProvider @Inject() extends Mappings {

  def apply(exemptionCount: Int): Form[AssessmentAnswer] = {
    val messagesKey = if (exemptionCount == 1) { "assessment.error.required.onlyOne" }
    else { "assessment.error.required" }

    Form(
      "value" ->
        seq(text(messagesKey))
          .verifying(messagesKey, values => values.nonEmpty)
          .verifying(
            messagesKey,
            values => !values.contains("false") || values.size == 1
          )
          .transform[AssessmentAnswer](
            values => AssessmentAnswer.fromStringOrSeq(Right(values)),
            {
              case AssessmentAnswer.NoExemption     => Seq("false")
              case AssessmentAnswer.Exemption(vals) => vals
              case _                                => Seq.empty
            }
          )
    )
  }
}
