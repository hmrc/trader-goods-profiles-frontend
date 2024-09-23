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

package models

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait AssessmentAnswer

object AssessmentAnswer {

  case object NoExemption extends WithName("false") with AssessmentAnswer
  case object Exemption extends WithName("true") with AssessmentAnswer

  // Unideal but need it as a placeholder when recategorising - because it stores answers in a JSON array
  case object NotAnsweredYet extends WithName("notAnswered") with AssessmentAnswer

  implicit val reads: Reads[AssessmentAnswer] = Reads {
    case JsString("false")       => JsSuccess(NoExemption)
    case JsString("true")        => JsSuccess(Exemption)
    case JsString("notAnswered") => JsSuccess(NotAnsweredYet)
    case _                       => JsError("unable to read assessment answer")
  }

  implicit val writes: Writes[AssessmentAnswer] = Writes {
    case Exemption      => JsString("true")
    case NotAnsweredYet => JsString("notAnswered")
    case NoExemption    => JsString("false")
  }

  def fromString(input: String): AssessmentAnswer =
    input match {
      case Exemption.toString      => Exemption
      case NotAnsweredYet.toString => NotAnsweredYet
      case _                       => NoExemption
    }
}
