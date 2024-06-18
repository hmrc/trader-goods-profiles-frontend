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

import models.RecordCategorisations
import play.api.libs.json.{Json, OFormat}

case class GetCategorisationAssessmentDetailsEventOutcome(
  status: String,
  statusCode: String,
  failureReason: String
)

object GetCategorisationAssessmentDetailsEventOutcome {
  implicit val format: OFormat[GetCategorisationAssessmentDetailsEventOutcome] =
    Json.format[GetCategorisationAssessmentDetailsEventOutcome]
}

case class GetCategorisationAssessmentDetailsEvent(
  eori: String,
  affinityGroup: String,
  recordId: String,
  commodityCode: String,
  countryOfOrigin: String,
  dateOfTrade: String,
  requestDateTime: String,
  responseDateTime: String,
  outcome: GetCategorisationAssessmentDetailsEventOutcome,
  categoryAssessmentOptions: String,
  exemptionOptions: String
)

object GetCategorisationAssessmentDetailsEvent {
  implicit val format: OFormat[GetCategorisationAssessmentDetailsEvent] =
    Json.format[GetCategorisationAssessmentDetailsEvent]
}
