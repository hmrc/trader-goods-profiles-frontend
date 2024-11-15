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

sealed trait AdviceStatusMessage {
  def messageKey: String
}

object AdviceStatusMessage {
  case object NotRequested extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.notRequested"
  }
  case object Requested extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.requested"
  }
  case object AdviceWithdrawn extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.adviceWithdrawn"
  }
  case object NotProvided extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.notProvided"
  }
  case object InProgress extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.inProgress"
  }
  case object InformationRequested extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.informationRequested"
  }
  case object AdviceReceived extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.adviceReceived"
  }

  def fromString(status: String): Option[AdviceStatusMessage] = status match {
    case "Not Requested"         => Some(NotRequested)
    case "Requested"             => Some(Requested)
    case "Advice withdrawn"      => Some(AdviceWithdrawn)
    case "Advice not provided"   => Some(NotProvided)
    case "In progress"           => Some(InProgress)
    case "Information requested" => Some(InformationRequested)
    case "Advice received"       => Some(AdviceReceived)
    case _                       => None
  }
}
