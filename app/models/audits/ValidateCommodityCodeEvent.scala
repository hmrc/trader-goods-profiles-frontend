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

package models.audits

import play.api.libs.json.{Json, OFormat}

case class ValidateCommodityCodeEventOutcome(
  commodityCodeStatus: String,
  status: String,
  statusCode: String,
  failureReason: Option[String]
)

object ValidateCommodityCodeEventOutcome {
  implicit val format: OFormat[ValidateCommodityCodeEventOutcome] = Json.format[ValidateCommodityCodeEventOutcome]
}

case class ValidateCommodityCodeEvent(
  eori: String,
  affinityGroup: String,
  journey: Option[String],
  recordId: Option[String],
  commodityCode: String,
  requestDateTime: String,
  responseDateTime: String,
  outcome: ValidateCommodityCodeEventOutcome,
  commodityDescription: Option[String],
  commodityCodeEffectiveTo: Option[String],
  commodityCodeEffectiveFrom: Option[String]
)

object ValidateCommodityCodeEvent {
  implicit val format: OFormat[ValidateCommodityCodeEvent] = Json.format[ValidateCommodityCodeEvent]
}
