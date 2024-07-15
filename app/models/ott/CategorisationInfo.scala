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
import play.api.libs.json.{Json, OFormat}

final case class CategorisationInfo(
  commodityCode: String,
  categoryAssessments: Seq[CategoryAssessment],
  measurementUnit: Option[String],
  descendantCount: Int,
  originalCommodityCode: Option[String] = None
) {
  private val padlength = 10

  def latestDoesNotMatchOriginal: Boolean = originalCommodityCode match {
    case Some(originalComcode) =>
      commodityCode != originalComcode.padTo[Char](padlength, '0').mkString
    case None =>
      false
  }
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
