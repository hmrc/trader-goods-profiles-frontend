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

package models.ott.response

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class OttResponse(
  goodsNomenclature: GoodsNomenclatureResponse,
  categoryAssessmentRelationships: Seq[CategoryAssessmentRelationship],
  includedElements: Seq[IncludedElement],
  descendents: Seq[Descendant]
) {

  lazy val themes: Seq[ThemeResponse] = includedElements.flatMap {
    case t: ThemeResponse => Some(t)
    case _                => None
  }

  lazy val categoryAssessments: Seq[CategoryAssessmentResponse] = includedElements.flatMap {
    case ca: CategoryAssessmentResponse => Some(ca)
    case _                              => None
  }

  lazy val certificates: Seq[CertificateResponse] = includedElements.flatMap {
    case c: CertificateResponse => Some(c)
    case _                      => None
  }

  lazy val additionalCodes: Seq[AdditionalCodeResponse] = includedElements.flatMap {
    case a: AdditionalCodeResponse => Some(a)
    case _                         => None
  }

  lazy val otherExemptions: Seq[OtherExemptionResponse] = includedElements.flatMap {
    case e: OtherExemptionResponse => Some(e)
    case _                         => None
  }

  lazy val legalAct: Seq[LegalActResponse] = includedElements.flatMap {
    case l: LegalActResponse => Some(l)
    case _                   => None
  }
}

object OttResponse {

  private val categoryAssessmentReads: Reads[Seq[CategoryAssessmentRelationship]] = {
    val applicableCategoryAssessments = (__ \ "data" \ "relationships" \ "applicable_category_assessments" \ "data")
      .read[Seq[CategoryAssessmentRelationship]]
    val descendantCategoryAssessments = (__ \ "data" \ "relationships" \ "descendant_category_assessments" \ "data")
      .read[Seq[CategoryAssessmentRelationship]]

    for {
      applicableCategoryAssessments <- applicableCategoryAssessments
      descendantCategoryAssessments <- descendantCategoryAssessments
    } yield applicableCategoryAssessments ++ descendantCategoryAssessments
  }

  implicit lazy val reads: Reads[OttResponse] = (
    __.read[GoodsNomenclatureResponse] and
      categoryAssessmentReads and
      (__ \ "included").read[Seq[IncludedElement]] and
      (__ \ "data" \ "relationships" \ "descendants" \ "data").read[Seq[Descendant]]
  )(OttResponse.apply _)
}
