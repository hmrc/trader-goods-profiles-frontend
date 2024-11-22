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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class AdviceStatusMessageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "AdviceStatusMessage" - {
    "return correct message keys for each status" in {
      AdviceStatusMessage.NotRequestedParagraph.messageKey mustBe "singleRecord.adviceParagraph.notRequested"
      AdviceStatusMessage.RequestedParagraph.messageKey mustBe "singleRecord.adviceParagraph.requested"
      AdviceStatusMessage.AdviceWithdrawnParagraph.messageKey mustBe "singleRecord.adviceParagraph.adviceWithdrawn"
      AdviceStatusMessage.NotProvidedParagraph.messageKey mustBe "singleRecord.adviceParagraph.adviceNotProvided"
      AdviceStatusMessage.InProgressParagraph.messageKey mustBe "singleRecord.adviceParagraph.inProgress"
      AdviceStatusMessage.InformationRequestedParagraph.messageKey mustBe "singleRecord.adviceParagraph.informationRequested"
      AdviceStatusMessage.AdviceReceivedParagraph.messageKey mustBe "singleRecord.adviceParagraph.adviceReceived"
    }
  }

  "map fromString to the correct case object for valid input strings" in {
    AdviceStatusMessage.fromString(NotRequested) mustBe Some(AdviceStatusMessage.NotRequestedParagraph)
    AdviceStatusMessage.fromString(Requested) mustBe Some(AdviceStatusMessage.RequestedParagraph)
    AdviceStatusMessage.fromString(AdviceRequestWithdrawn) mustBe Some(AdviceStatusMessage.AdviceWithdrawnParagraph)
    AdviceStatusMessage.fromString(InProgress) mustBe Some(AdviceStatusMessage.InProgressParagraph)
    AdviceStatusMessage.fromString(InformationRequested) mustBe Some(AdviceStatusMessage.InformationRequestedParagraph)
    AdviceStatusMessage.fromString(AdviceReceived) mustBe Some(AdviceStatusMessage.AdviceReceivedParagraph)
  }
}
