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

import models.AdviceStatus._

sealed trait AdviceStatusMessage {
  def messageKey: String
}

object AdviceStatusMessage {
  case object NotRequestedParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.notRequested"
  }
  case object RequestedParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.requested"
  }
  case object AdviceWithdrawnParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.adviceWithdrawn"
  }
  case object NotProvidedParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.adviceNotProvided"
  }
  case object InProgressParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.inProgress"
  }
  case object InformationRequestedParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.informationRequested"
  }
  case object AdviceReceivedParagraph extends AdviceStatusMessage {
    val messageKey: String = "singleRecord.adviceParagraph.adviceReceived"
  }

  def fromString(status: AdviceStatus): Option[AdviceStatusMessage] = status match {
    case NotRequested        => Some(NotRequestedParagraph)
    case Requested            => Some(RequestedParagraph)
    case AdviceRequestWithdrawn      => Some(AdviceWithdrawnParagraph)
    case AdviceNotProvided  => Some(NotProvidedParagraph)
    case InProgress          => Some(InProgressParagraph)
    case InformationRequested => Some(InformationRequestedParagraph)
    case AdviceReceived      => Some(AdviceReceivedParagraph)
    case _                       => None
  }
}
