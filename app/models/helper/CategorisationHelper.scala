package models.helper

import models.UserAnswers

object CategorisationHelper {

  def getScenario(userAnswers: UserAnswers): Scenario.Value = {
    //TODO: Implement logic
    Scenario.Category1NoExemptions
  }

}

object Scenario extends Enumeration {
  type Scenario = Value

  val Category1NoExemptions, Category3NoAssessments, StandardNoSupplementaryUnits, Category1, Category2 = Value
}
