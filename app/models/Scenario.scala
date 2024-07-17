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

import models.ott.{CategorisationInfo, ExemptionType}
import play.api.mvc.JavascriptLiteral

sealed trait Scenario

case object NoRedirectScenario extends Scenario
case object Category1NoExemptions extends Scenario
case object StandardNoAssessments extends Scenario
case object NiphlsOnly extends Scenario
case object NiphlsAndOthers extends Scenario

case object Standard extends Scenario
case object Category1 extends Scenario
case object Category2 extends Scenario

object Scenario {

  def getRedirectScenarios(categorisationInfo: CategorisationInfo): Scenario = {
    val hasCategoryAssessments: Boolean =
      categorisationInfo.categoryAssessments.nonEmpty

    val category1Assessments = categorisationInfo.categoryAssessments.filter(_.category == 1)
    val category2Assessments = categorisationInfo.categoryAssessments.filter(_.category == 2)

    val hasCategory1Assessments: Boolean =
      category1Assessments.nonEmpty

    val hasEveryCategory1AssessmentGotExemptions: Boolean =
      category1Assessments.count(assessment => assessment.exemptions.nonEmpty) == category1Assessments.size

    val niphlsAssessment =
      category1Assessments.exists(assessment => assessment.exemptions.exists(exemption => exemption.exemptionType ==ExemptionType.OtherExemption && exemption.code == "WFE012")) && category2Assessments.size == 1 && category2Assessments.exists(assessment => assessment.exemptions.isEmpty)

    //TODO duplicate logic
    val hasOnlyNiphlsExemptionInCategory1: Boolean =
      category1Assessments.count(assessment => assessment.exemptions.exists(exemption =>
        exemption.exemptionType == ExemptionType.OtherExemption && exemption.code == "WFE012")).equals(category1Assessments.size)

    (hasCategoryAssessments, hasCategory1Assessments, hasEveryCategory1AssessmentGotExemptions, niphlsAssessment, hasOnlyNiphlsExemptionInCategory1) match {
      case (true, true, true, true, true) => NiphlsOnly
      case (true, true, true, true, false) => NiphlsAndOthers
      case (true, true, false, _, _)   => Category1NoExemptions
      case (false, _, _, _, _) => StandardNoAssessments
      case _            => NoRedirectScenario
    }
  }

  def getScenario(goodsRecord: CategoryRecord): Scenario =
    goodsRecord.category match {
      case 1 => Category1
      case 2 => Category2
      case 3 => Standard
    }

  implicit val jsLiteral: JavascriptLiteral[Scenario] = {
    case NoRedirectScenario    => "NoRedirectScenario"
    case Category1NoExemptions => "Category1NoExemptions"
    case StandardNoAssessments => "StandardNoAssessments"
    case Standard              => "Standard"
    case Category1             => "Category1"
    case Category2             => "Category2"
  }
}
