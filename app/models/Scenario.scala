/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.mvc.JavascriptLiteral
import utils.Constants._

// Base trait for all scenarios
sealed trait Scenario {
  def isAutoCategorised: Boolean = false
  def isManualCategorised: Boolean = false
}

// Auto-categorised scenarios
sealed trait ScenarioAuto extends Scenario {
  override val isAutoCategorised: Boolean = true
}
case object StandardGoodsScenario extends ScenarioAuto with ScenarioCategorised {
  override def messageKey: String = "singleRecord.standardGoods"
}
case object Category1Scenario extends ScenarioAuto with ScenarioCategorised {
  override def messageKey: String = "singleRecord.cat1"
}
case object Category2Scenario extends ScenarioAuto with ScenarioCategorised {
  override def messageKey: String = "singleRecord.cat2"
}

// Manual-categorised scenarios
sealed trait ScenarioManual extends Scenario {
  override val isManualCategorised: Boolean = true
}
case object StandardGoodsNoAssessmentsScenario extends ScenarioManual with ScenarioCategorised {
  override def messageKey: String = "singleRecord.standardGoods"
}
case object Category1NoExemptionsScenario extends ScenarioManual with ScenarioCategorised {
  override def messageKey: String = "singleRecord.cat1"
}
case object Category2NoExemptionsScenario extends ScenarioManual with ScenarioCategorised {
  override def messageKey: String = "singleRecord.cat2"
}

// Represents no category
case object EmptyScenario extends Scenario

object Scenario {

  // Return Option[Int] for any scenario
  def getResultAsInt(scenario: Scenario): Option[Int] =
    scenario match {
      case StandardGoodsScenario | StandardGoodsNoAssessmentsScenario => Some(StandardGoodsAsInt)
      case Category1Scenario | Category1NoExemptionsScenario         => Some(Category1AsInt)
      case Category2Scenario | Category2NoExemptionsScenario         => Some(Category2AsInt)
      case EmptyScenario                                             => None
    }

  // Convert Option[Int] back to Scenario
  def fromInt(optInt: Option[Int]): Option[Scenario] =
    optInt match {
      case Some(StandardGoodsAsInt) => Some(StandardGoodsScenario)
      case Some(Category1AsInt)     => Some(Category1Scenario)
      case Some(Category2AsInt)     => Some(Category2Scenario)
      case None                     => Some(EmptyScenario)
      case _                        => None
    }

  // JS literal for frontend
  implicit val jsLiteral: JavascriptLiteral[Scenario] = {
    case StandardGoodsScenario               => "Standard"
    case Category1Scenario                   => "Category1"
    case Category2Scenario                   => "Category2"
    case StandardGoodsNoAssessmentsScenario => "StandardNoAssessments"
    case Category1NoExemptionsScenario      => "Category1NoExemptions"
    case Category2NoExemptionsScenario      => "Category2NoExemptions"
    case EmptyScenario                       => ""
  }
}
