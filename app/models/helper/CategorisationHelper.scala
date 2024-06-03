package models.helper

import models.UserAnswers

object CategorisationHelper {

  def getScenario(userAnswers: UserAnswers): Scenario = {
    //TODO: Implement logic
    Category1NoExemptions
  }

}

sealed trait Scenario

case object Category1NoExemptions extends Scenario
case object Category3NoAssessments extends Scenario
case object StandardNoSupplementaryUnits extends Scenario
case object Category1 extends Scenario
case object Category2 extends Scenario
