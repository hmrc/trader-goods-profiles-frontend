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

sealed trait AdviceStatus {
  val messageKey: String
}

object AdviceStatus {

  val values: Seq[AdviceStatus] = Seq(
    NotRequested,
    Requested,
    InProgress,
    InformationRequested,
    AdviceReceived,
    AdviceNotProvided,
    AdviceRequestWithdrawn
  )

  case object NotRequested extends AdviceStatus {
    val messageKey = "adviceStatus.notRequested"
  }

  case object Requested extends AdviceStatus {
    val messageKey = "adviceStatus.requested"
  }

  case object InProgress extends AdviceStatus {
    val messageKey = "adviceStatus.inProgress"
  }

  case object InformationRequested extends AdviceStatus {
    val messageKey = "adviceStatus.informationRequested"
  }

  case object AdviceReceived extends AdviceStatus {
    val messageKey = "adviceStatus.adviceReceived"
  }

  case object AdviceNotProvided extends AdviceStatus {
    val messageKey = "adviceStatus.adviceNotProvided"
  }

  case object AdviceRequestWithdrawn extends AdviceStatus {
    val messageKey = "adviceStatus.adviceRequestWithdrawn"
  }

  implicit val writes: Writes[AdviceStatus] = Writes[AdviceStatus] {
    case NotRequested           => JsString("Not Requested")
    case Requested              => JsString("Requested")
    case InProgress             => JsString("In progress")
    case InformationRequested   => JsString("Information Requested")
    case AdviceReceived         => JsString("Advice Provided")
    case AdviceNotProvided      => JsString("Advice not provided")
    case AdviceRequestWithdrawn => JsString("Advice request withdrawn")
  }

  implicit val reads: Reads[AdviceStatus] = Reads[AdviceStatus] {
    case JsString("Not Requested")            => JsSuccess(NotRequested)
    case JsString("Requested")                => JsSuccess(Requested)
    case JsString("In progress")              => JsSuccess(InProgress)
    case JsString("Information Requested")    => JsSuccess(InformationRequested)
    case JsString("Advice Provided")          => JsSuccess(AdviceReceived)
    case JsString("Advice not provided")      => JsSuccess(AdviceNotProvided)
    case JsString("Advice request withdrawn") => JsSuccess(AdviceRequestWithdrawn)
    case other                                => JsError(s"[AdviceStatus] Reads unknown AdviceStatus: $other")
  }

}
