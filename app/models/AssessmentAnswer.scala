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

import play.api.libs.json._

sealed trait AssessmentAnswer

object AssessmentAnswer {

  case object NoExemption extends WithName("none") with AssessmentAnswer

  case class Exemption(values: Seq[String]) extends AssessmentAnswer

  case object NotAnsweredYet extends WithName("notAnswered") with AssessmentAnswer

  implicit val reads: Reads[AssessmentAnswer] = Reads {
    case JsString("none")        => JsSuccess(NoExemption)
    case JsString("notAnswered") => JsSuccess(NotAnsweredYet)
    case JsArray(values)         => JsSuccess(Exemption(values.map(_.as[String]).toSeq))
    case _                       => JsError("unable to read assessment answer")
  }

  implicit val writes: Writes[AssessmentAnswer] = Writes {
    case Exemption(values) => JsArray(values.map(JsString))
    case NotAnsweredYet    => JsString("notAnswered")
    case NoExemption       => JsString("none")
  }

  def fromSeq(input: Seq[String]): AssessmentAnswer =
    if (input.contains(NoExemption.toString)) {
      NoExemption
    } else {
      Exemption(input)
    }

  def toSeq(answer: AssessmentAnswer): Seq[String] = answer match {
    case NoExemption     => Seq(NoExemption.toString)
    case NotAnsweredYet  => Seq.empty
    case Exemption(vals) => vals
  }

}
