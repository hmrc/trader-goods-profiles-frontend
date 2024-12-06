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

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

sealed trait DeclarableStatus {
  val messageKey: String
  val paragraphKey: String
  val paragraphTagColour: String
}

object DeclarableStatus {

  val values: Seq[DeclarableStatus] = Seq(
    ImmiReady,
    NotReadyForImmi,
    NotReadyForUse
  )

  case object ImmiReady extends DeclarableStatus {
    val messageKey         = "declarableStatus.immiReady"
    val paragraphKey       = "declarableStatus.immiReady.paragraph"
    val paragraphTagColour = "govuk-tag--green"
  }

  case object NotReadyForImmi extends DeclarableStatus {
    val messageKey         = "declarableStatus.notReadyForImmi"
    val paragraphKey       = "declarableStatus.notReadyForImmi.paragraph"
    val paragraphTagColour = "govuk-tag--orange"
  }

  case object NotReadyForUse extends DeclarableStatus {
    val messageKey         = "declarableStatus.notReadyForUse"
    val paragraphKey       = "declarableStatus.notReadyForUse.paragraph"
    val paragraphTagColour = "govuk-tag--red"
  }

  implicit val writes: Writes[DeclarableStatus] = Writes[DeclarableStatus] {
    case ImmiReady       => JsString("IMMI Ready")
    case NotReadyForImmi => JsString("Not ready for IMMI")
    case NotReadyForUse  => JsString("Not Ready For Use")
  }

  implicit val reads: Reads[DeclarableStatus] = Reads[DeclarableStatus] {
    case JsString("IMMI Ready")         => JsSuccess(ImmiReady)
    case JsString("Not ready for IMMI") => JsSuccess(NotReadyForImmi)
    case JsString("Not Ready For Use")  => JsSuccess(NotReadyForUse)
    case other                          => JsError(s"[DeclarableStatus] Reads unknown DeclarableStatus: $other")
  }

}
