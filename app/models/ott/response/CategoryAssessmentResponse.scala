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
import play.api.libs.json._

final case class CategoryAssessmentResponse(
  id: String,
  themeId: String,
  exemptions: Seq[ExemptionResponse]
) extends IncludedElement

object CategoryAssessmentResponse {

  implicit lazy val reads: Reads[CategoryAssessmentResponse] =
    (
      (__ \ "id").read[String] and
        (__ \ "relationships" \ "theme" \ "data" \ "id").read[String] and
        (__ \ "relationships" \ "exemptions" \ "data").read[Seq[ExemptionResponse]]
    )(CategoryAssessmentResponse.apply _)
}
