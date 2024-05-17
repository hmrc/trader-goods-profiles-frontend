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

package models.ott

import cats.implicits.toTraverseOps
import models.ott.response.OttResponse

final case class CategoryAssessment(
                                     id: String,
                                     category: Int,
                                     exemptions: Seq[Exemption]
                                   )

object CategoryAssessment {

  def build(id: String, ottResponse: OttResponse): Option[CategoryAssessment] =
    for {
      assessment <- ottResponse.categoryAssessments.find(_.id == id)
      theme      <- ottResponse.themes.find(_.id == assessment.themeId)
      exemptions <- assessment.exemptions.map(x => buildExemption(x.id, ottResponse)).sequence
    } yield CategoryAssessment(id, theme.category, exemptions)

  def buildExemption(id: String, ottResponse: OttResponse): Option[Exemption] =
    ottResponse.exemptions
      .find(_.id == id)
      .map(x => Exemption(x.id, x.code, x.description))
}