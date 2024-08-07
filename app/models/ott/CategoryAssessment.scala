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
import models.ott.response.{ExemptionType, OttResponse}
import play.api.libs.json.{Json, OFormat}
import utils.Constants.Category1AsInt

final case class CategoryAssessment(
  id: String,
  category: Int,
  exemptions: Seq[Exemption]
) extends Ordered[CategoryAssessment] {

  import scala.math.Ordered.orderingToOrdered

  override def compare(that: CategoryAssessment): Int =
    (this.category, this.exemptions.size) compare (that.category, that.exemptions.size)

  def getExemptionListItems: Seq[String] = exemptions.map { exemption =>
    exemption.code + " - " + exemption.description
  }

  def isCategory1: Boolean  = category == Category1AsInt
  def hasNoAnswers: Boolean = exemptions.isEmpty

}

object CategoryAssessment {

  def build(id: String, ottResponse: OttResponse): Option[CategoryAssessment] =
    for {
      assessment <- ottResponse.categoryAssessments.find(_.id == id)
      theme      <- ottResponse.themes.find(_.id == assessment.themeId)
      exemptions <- assessment.exemptions.map(x => buildExemption(x.id, x.exemptionType, ottResponse)).sequence
    } yield CategoryAssessment(id, theme.category, exemptions)

  private def buildExemption(id: String, exemptionType: ExemptionType, ottResponse: OttResponse): Option[Exemption] =
    exemptionType match {
      case ExemptionType.Certificate =>
        ottResponse.certificates
          .find(_.id == id)
          .map(x => Certificate(x.id, x.code, x.description))

      case ExemptionType.AdditionalCode =>
        ottResponse.additionalCodes
          .find(_.id == id)
          .map(x => AdditionalCode(x.id, x.code, x.description))

      case ExemptionType.OtherExemption =>
        ottResponse.otherExemptions
          .find(_.id == id)
          .map(x => OtherExemption(x.id, x.code, x.description))
    }

  implicit lazy val format: OFormat[CategoryAssessment] = Json.format
}
