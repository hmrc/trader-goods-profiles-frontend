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
import models.AssessmentAnswer.NotAnsweredYet
import models.UserAnswers
import models.ott.response.OttResponse
import pages.AssessmentPage
import play.api.libs.json.{Json, OFormat}

final case class CategorisationInfo(
  commodityCode: String,
  categoryAssessments: Seq[CategoryAssessment],
  measurementUnit: Option[String],
  descendantCount: Int,
  originalCommodityCode: Option[String] = None
  //TODO needs country for comparisions??
) {
  def areThereAnyNonAnsweredQuestions(recordId: String, userAnswers: UserAnswers): Boolean =
    categoryAssessments.indices
      .map(index => userAnswers.get(AssessmentPage(recordId, index)))
      .count(x => x.contains(NotAnsweredYet)) > 0

}

object CategorisationInfo {

  def build(ott: OttResponse, originalCommodityCode: Option[String] = None): Option[CategorisationInfo] =
    ott.categoryAssessmentRelationships
      .map(x => CategoryAssessment.build(x.id, ott))
      .sequence
      .map { assessments =>
        CategorisationInfo(
          ott.goodsNomenclature.commodityCode,
          assessments.sorted,
          ott.goodsNomenclature.measurementUnit,
          ott.descendents.size,
          originalCommodityCode
        )
      }

  implicit lazy val format: OFormat[CategorisationInfo] = Json.format
}
