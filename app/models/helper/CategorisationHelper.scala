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

package models.helper

import models.router.responses.GetGoodsRecordResponse

object CategorisationHelper {

  def getScenario(goodsRecord: GetGoodsRecordResponse): Scenario =
    goodsRecord.category match {
      case 1 => Category1
      case 2 => Category2
      case 3 =>
        goodsRecord.supplementaryUnit match {
          case Some(_) => throw new Exception(s"${goodsRecord.recordId}: Category 3 with supplementary units")
          case None    => StandardNoSupplementaryUnits
        }
    }
}

sealed trait Scenario

case object Category1NoExemptions extends Scenario
case object Category3NoAssessments extends Scenario
case object StandardNoSupplementaryUnits extends Scenario
case object Category1 extends Scenario
case object Category2 extends Scenario
