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

package models.ott.response

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, __}

final case class ExemptionResponse(id: String, exemptionType: ExemptionType)

object ExemptionResponse {

  implicit lazy val reads: Reads[ExemptionResponse] = (
    (__ \ "id").read[String] and
      (__ \ "type").read[ExemptionType]
  )(ExemptionResponse.apply _)
}

sealed trait ExemptionType

object ExemptionType {

  case object Certificate extends ExemptionType
  case object AdditionalCode extends ExemptionType
  case object OtherExemption extends ExemptionType

  implicit lazy val reads: Reads[ExemptionType] = Reads {
    case JsString("certificate")     => JsSuccess(Certificate)
    case JsString("additional_code") => JsSuccess(AdditionalCode)
    case JsString("exemption")       => JsSuccess(OtherExemption)
    case _                           => JsError("unable to parse exemption type")
  }
}
