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
import java.time.Instant

case class OttResponse(
  data: JsObject,
  included: List[JsObject]
) {
  def getApplicableCategoryAssessments(): List[CategoryAssessment] = {
    val applicableAssesmentReferences = (data \ "relationships" \ "applicable_category_assessments" \ "data").as[List[JsObject]]
    val includedAssesments = getIncludedFromReferences(applicableAssesmentReferences)
    includedAssesments.map { assessment =>
      CategoryAssessment(
        id = (assessment \ "id").asOpt[String],
        category = (assessment \ "attributes" \ "category").asOpt[String],
        theme = (assessment \ "attributes" \ "theme").asOpt[String],
        geographicalArea = (assessment \ "relationships" \ "geographical_area").asOpt[JsObject],
        excludedGeographicalAreas = (assessment \ "relationships" \ "excluded_geographical_areas").asOpt[JsObject],
        exemptions = Some(getExemptions(assessment)),
        measures = Some(getMeasures(assessment))
      )
    }
  }

  private def getMeasures(assessment: JsObject): List[Measure] = {
    val measureReferences = (assessment \ "relationships" \ "measures" \ "data").as[List[JsObject]]
    val includedMeasures = getIncludedFromReferences(measureReferences)
    includedMeasures.map { measure =>
      Measure(
        id = (measure \ "id").asOpt[String],
        goodsNomenclatureItemId = (measure \ "attributes" \ "goods_nomenclature_item_id").asOpt[String],
        goodsNomenclatureSid = (measure \ "attributes" \ "goods_nomenclature_sid").asOpt[String],
        effectiveStartDate = (measure \ "attributes" \ "effective_start_date").asOpt[Instant],
        effectiveEndDate = (measure \ "attributes" \ "effective_end_Date").asOpt[Instant],
        measureType = Some(getMeasureType(measure)),
        footnotes = None
      )
    }
  }

  private def getMeasureType(measure: JsObject): MeasureType = {
    val measureTypeReferences = (measure \ "relationships" \ "measure_type" \ "data").asOpt[JsObject].toList
    val measureTypes = getIncludedFromReferences(measureTypeReferences)
    if (measureTypes.length != 0) {
      val measureType = measureTypes(0)
      MeasureType(
        id = (measureType \ "id").asOpt[String],
        description = (measureType \ "attributes" \ "description").asOpt[String],
        measureTypeSeriesDescription = (measureType \ "attributes" \ "measure_type_series_description").asOpt[String],
        validityStartDate = (measureType \ "attributes" \ "validity_start_date").asOpt[Instant],
        validityEndDate = (measureType \ "attributes" \ "validity_end_date").asOpt[Instant],
        measureTypeSeriesId = (measureType \ "attributes" \ "measure_type_series_id").asOpt[String],
        tradeMovementCode = (measureType \ "attributes" \ "trade_movement_code").asOpt[Int]
      )
    } else {
      MeasureType(
        id = (measure \ "relationships" \ "measure_type" \ "data" \ "id").asOpt[String]
      )
    }
  }

  private def getExemptions(assessment: JsObject): List[Either[Certificate, AdditionalCode]] = {
    val exemptionReferences = (assessment \ "relationships" \ "exemptions" \ "data").as[List[JsObject]]
    val includedExemptions = getIncludedFromReferences(exemptionReferences)
    includedExemptions.map { exemption =>
      if ((exemption \ "type").as[String] == "certificate") {
        Left(
          Certificate(
            id = (exemption \ "id").asOpt[String],
            certificateTypeCode = (exemption \ "attributes" \ "certificate_type_code").asOpt[String],
            certificateCode = (exemption \ "attributes" \ "certificate_code").asOpt[String],
            code = (exemption \ "attributes" \ "code").asOpt[String],
            description = (exemption \ "attributes" \ "description").asOpt[String],
            formattedDescription = (exemption \ "attributes" \ "formatted_description").asOpt[String]
          )
        )
      } else {
        Right(
          AdditionalCode(
            id = (exemption \ "id").asOpt[String],
            additionalCodeTypeId = (exemption \ "attributes" \ "additional_code_type_id").asOpt[String],
            additionalCode = (exemption \ "attributes" \ "additional_code_type_id").asOpt[String],
            code = (exemption \ "attributes" \ "code").asOpt[String],
            description = (exemption \ "attributes" \ "description").asOpt[String],
            formattedDescription = (exemption \ "attributes" \ "formatted_description").asOpt[String]
          )
        )
      }
    }
  }

  private def getIncludedFromReferences(references: List[JsObject]): List[JsObject] = {
    included.filter(obj => references.map(obj => (obj \ "id").as[String]).contains((obj \ "id").as[String]))
  }
}

object OttResponse {
  implicit val ottResponseFormat = Json.format[OttResponse]
}