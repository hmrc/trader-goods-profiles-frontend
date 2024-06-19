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

import models.helper.Journey
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.LocalDate
case class OttAuditData(
  auditMode: OttAuditMode,
  eori: String,
  affinityGroup: AffinityGroup,
  recordId: Option[String],
  commodityCode: String,
  countryOfOrigin: Option[String],
  dateOfTrade: Option[LocalDate],
  journey: Option[Journey]
)

sealed trait OttAuditMode

case object AuditValidateCommodityCode extends OttAuditMode
case object AuditGetCategorisationAssessment extends OttAuditMode
