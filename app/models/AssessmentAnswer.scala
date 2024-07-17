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

  case object NoExemption extends WithName("none") with AssessmentAnswer
  final case class Exemption(id: String) extends AssessmentAnswer { override val toString: String = id }

  // Unideal but need it as a placeholder when recategorising
  case object NotAnsweredYet extends WithName("notAnswered") with AssessmentAnswer

  implicit val reads: Reads[AssessmentAnswer] = Reads {
    case JsString("none") => JsSuccess(NoExemption)
    case JsString("notAnswered") => JsSuccess(NotAnsweredYet)
    case JsString(s)      => JsSuccess(Exemption(s))
    case _                => JsError("unable to read assessment answer")
  }

  implicit val writes: Writes[AssessmentAnswer] = Writes {
    case NoExemption  => JsString("none")
    case Exemption(s) => JsString(s)
    case NotAnsweredYet => JsString("notAnswered")
  }

  def fromString(input: String): AssessmentAnswer =
    input match {
      case NoExemption.toString => NoExemption
      case NotAnsweredYet.toString => NotAnsweredYet
      case s                    => Exemption(s)
    }

  def radioOptions(exemptions: Seq[ott.Exemption])(implicit messages: Messages): Seq[RadioItem] =
    exemptions.distinct.zipWithIndex.map { case (exemption, index) =>
      RadioItem(
        content = Text(messages("assessment.exemption", exemption.code, exemption.description)),
        value = Some(exemption.id),
        id = Some(s"value_$index")
      )
    } :+ RadioItem(divider = Some(messages("site.or"))) :+ RadioItem(
      content = Text(messages("assessment.exemption.none")),
      value = Some(NoExemption.toString),
      id = Some(s"value_${exemptions.size}")
    )
}
