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

import play.api.libs.json._

trait IncludedElement

object IncludedElement {

  implicit lazy val reads: Reads[IncludedElement] =
    (__ \ "type")
      .read[String]
      .flatMap[IncludedElement] {
        case "category_assessment" => CategoryAssessmentResponse.reads.widen
        case "theme"               => ThemeResponse.reads.widen
        case "certificate"         => CertificateResponse.reads.widen
        case "additional_code"     => AdditionalCodeResponse.reads.widen
        case "exemption"           => OtherExemptionResponse.reads.widen
        case "measure_type"        => MeasureTypeResponse.reads.widen
        case _                     => Ignorable.reads.widen
      }
}
