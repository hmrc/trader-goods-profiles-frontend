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
import utils.Constants.{Category1AsInt, Category2AsInt, NiphlCode, NirmsCode}

final case class CategoryAssessment(
  id: String,
  category: Int,
  exemptions: Seq[Exemption],
  themeDescription: String,
  regulationUrl: Option[String]
) extends Ordered[CategoryAssessment] {

  import scala.math.Ordered.orderingToOrdered

  override def compare(that: CategoryAssessment): Int =
    (this.category, this.exemptions.size) compare (that.category, that.exemptions.size)

  def getCodesZippedWithDescriptions: Seq[(String, String)] = exemptions.map(_.code).zip(exemptions.map(_.description))

  def isCategory1: Boolean   = category == Category1AsInt
  def isCategory2: Boolean   = category == Category2AsInt
  def hasNoAnswers: Boolean  = exemptions.isEmpty // Refactor to be hasNoExemptions
  def hasAnswers: Boolean    = exemptions.nonEmpty // Refactor to be hasExemptions
  def isNiphlAnswer: Boolean = isCategory1 && exemptions.exists(exemption => exemption.id == NiphlCode)
  def isNirmsAnswer: Boolean = isCategory2 && exemptions.exists(exemption => exemption.id == NirmsCode)

  def onlyContainsNiphlAnswer: Boolean = isNiphlAnswer && exemptions.size == 1
  def onlyContainsNirmsAnswer: Boolean = isNirmsAnswer && exemptions.size == 1

  def isNiphlAsessmentAndTraderAuthorised(isAuthorised: Boolean): Boolean = isNiphlAnswer && isAuthorised
  def isNirmsAsessmentAndTraderAuthorised(isAuthorised: Boolean): Boolean = isNirmsAnswer && isAuthorised

}

object CategoryAssessment {

  def build(id: String, ottResponse: OttResponse): Option[CategoryAssessment] =
    for {
      assessment       <- ottResponse.categoryAssessments.find(_.id == id)
      theme            <- ottResponse.themes.find(_.id == assessment.themeId)
      exemptions       <- assessment.exemptions.map(x => buildExemption(x.id, x.exemptionType, ottResponse)).sequence
      themeDescription <- ottResponse.themes.find(_.id == assessment.themeId).map(_.theme)
      regulationUrl     = ottResponse.legalAct
                            .find(legalAct => legalAct.id.contains(assessment.regulationId))
                            .flatMap(_.regulationUrl)
    } yield CategoryAssessment(id, theme.category, exemptions, themeDescription, regulationUrl)

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
