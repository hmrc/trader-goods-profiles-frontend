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

import play.api.libs.json._
import scala.collection.mutable.ArrayBuffer

case class OttResponse(
  data: Data,
  included: Option[List[Data]]
) {


  private def extractMeasuresForCategoryAssessment(categoryAssessmentId: String): Option[List[Measure]] = {
    val relatedMeasureData = included.map { includedData =>
      includedData.filter(_.id == categoryAssessmentId)
    }

    val measureBuffer = ArrayBuffer[Measure]()

    for {
      dataList <- relatedMeasureData
      x <- dataList
    } {
      measureBuffer += Measure(
        x.id,
        data.attributes.flatMap(_.get("goods_nomenclature_item_id")).map {x => x.toString()},
        None,
        None,
        None,
        None,
        None
      )
    }

    if (measureBuffer.isEmpty) {
      None
    } else {
      Some(measureBuffer.toList)
    }
  }


  def extractCategoryAssessments(): Option[List[CategoryAssessment]] = {
    val referencesToAssesmentData = data.relationships.flatMap { rels =>
      rels.get("applicable_category_assessments").map { categoryAssessments =>
        categoryAssessments.values.flatMap {
          case Left(data) => Some(List(data))
          case Right(list) => Some(list)
        }.toList.flatten
      }
    }

    val referencedAssessmentData = included.flatMap { includedData =>
      referencesToAssesmentData.map { references =>
        references.flatMap { reference =>
          includedData.find(_.id == reference.id)
        }
      }
    }

    val categoryAssessmentsBuffer = ArrayBuffer[CategoryAssessment]()

    for {
      dataList <- referencedAssessmentData
      data <- dataList
    } {
      categoryAssessmentsBuffer += CategoryAssessment(
        data.id,
        data.attributes.flatMap(_.get("category")).get.toString(),
        data.attributes.flatMap(_.get("theme")),
        data.relationships.flatMap(_.get("geographical_area")),
        data.relationships.flatMap(_.get("excluded_geographical_areas")),
        data.relationships.flatMap(_.get("exemptions")),
        extractMeasuresForCategoryAssessment(data.id)
      )
    }

    if (categoryAssessmentsBuffer.isEmpty)  {
      None
    } else {
      Some(categoryAssessmentsBuffer.toList)
    }
  }
}

object OttResponse {
  implicit val goodsNomenclatureFormat: Format[OttResponse] = Json.format[OttResponse]
}
