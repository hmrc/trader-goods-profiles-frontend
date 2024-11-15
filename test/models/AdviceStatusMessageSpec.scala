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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class AdviceStatusMessageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "AdviceStatusMessage" - {
    "return correct message keys for each status" in {
      AdviceStatusMessage.NotRequested.messageKey mustBe "singleRecord.adviceParagraph.notRequested"
      AdviceStatusMessage.Requested.messageKey mustBe "singleRecord.adviceParagraph.requested"
      AdviceStatusMessage.AdviceWithdrawn.messageKey mustBe "singleRecord.adviceParagraph.adviceWithdrawn"
      AdviceStatusMessage.NotProvided.messageKey mustBe "singleRecord.adviceParagraph.notProvided"
      AdviceStatusMessage.InProgress.messageKey mustBe "singleRecord.adviceParagraph.inProgress"
      AdviceStatusMessage.InformationRequested.messageKey mustBe "singleRecord.adviceParagraph.informationRequested"
      AdviceStatusMessage.AdviceReceived.messageKey mustBe "singleRecord.adviceParagraph.adviceReceived"
    }
  }

  "map fromString to the correct case object for valid input strings" in {
    AdviceStatusMessage.fromString("Not Requested") mustBe Some(AdviceStatusMessage.NotRequested)
    AdviceStatusMessage.fromString("Requested") mustBe Some(AdviceStatusMessage.Requested)
    AdviceStatusMessage.fromString("Advice withdrawn") mustBe Some(AdviceStatusMessage.AdviceWithdrawn)
    AdviceStatusMessage.fromString("Advice not provided") mustBe Some(AdviceStatusMessage.NotProvided)
    AdviceStatusMessage.fromString("In progress") mustBe Some(AdviceStatusMessage.InProgress)
    AdviceStatusMessage.fromString("Information requested") mustBe Some(AdviceStatusMessage.InformationRequested)
    AdviceStatusMessage.fromString("Advice received") mustBe Some(AdviceStatusMessage.AdviceReceived)
  }

  "return None for invalid input strings" in {
    AdviceStatusMessage.fromString("Unknown status") mustBe None
    AdviceStatusMessage.fromString("") mustBe None
    AdviceStatusMessage.fromString("Completed") mustBe None
  }
}
