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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class RecordsSummary(
  eori: String,
  currentUpdate: Option[RecordsSummary.Update],
  lastUpdated: Instant
)

object RecordsSummary extends MongoJavatimeFormats.Implicits {

  final case class Update(recordsStored: Int, recordsToStore: Int)

  object Update {
    implicit lazy val format: OFormat[Update] = Json.format
  }

  implicit lazy val format: OFormat[RecordsSummary] = Json.format
}
