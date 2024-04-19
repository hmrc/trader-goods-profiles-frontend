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

package models.util

import models.CategoryAssessment
import models.ott.{AdditionalCode, Certificate, Exemption, GoodsNomenclature}
import models.ott.util.OttJsonApiParser
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util
import java.util.Collections

class OttJsonApiParserSpec extends AnyFreeSpec {

  "OttJsonApiParser" - {


    "should correctly parse a simple OTT response to a GoodsNomenclature object" in {
      val ottResponse = "{\"data\": {\"id\": \"123\", \"type\": \"goods_nomenclature\"}}"

      val expectedGoodsNomenclature = new GoodsNomenclature;
      expectedGoodsNomenclature.id = "123";

      val actualGoodsNomenclature = OttJsonApiParser.parse(ottResponse)

      expectedGoodsNomenclature.id shouldEqual actualGoodsNomenclature.id
    }

    "should correctly parse a complex OTT response to a GoodsNomenclature object" in {
      val ottResponse =
        """
          |{
          |  "data": {
          |    "id": "106662",
          |    "type": "goods_nomenclature",
          |    "attributes": {
          |      "goods_nomenclature_item_id": "2404120000"
          |    },
          |    "relationships": {
          |      "applicable_category_assessments": {
          |        "data": [
          |          {
          |            "id": "123456cd",
          |            "type": "category_assessment"
          |          }
          |        ]
          |      }
          |    }
          |  },
          |  "included": [
          |    {
          |      "id": "123456cd",
          |      "type": "category_assessment",
          |      "relationships": {
          |        "measures": {
          |          "data": [
          |            {
          |              "id": "3871194",
          |              "type": "measure"
          |            }
          |          ]
          |        },
          |        "exemptions": {
          |          "data": [
          |            {
          |              "id": "D005",
          |              "type": "certificate"
          |            },
          |            {
          |              "id": "3200",
          |              "type": "additional_code"
          |            }
          |          ]
          |        }
          |      }
          |    },
          |    {
          |      "id": "3871194",
          |      "type": "measure",
          |      "attributes": {
          |        "goods_nomenclature_item_id": "2404120000",
          |        "goods_nomenclature_sid": "106662",
          |        "effective_start_date": "2022-01-01T00:00:00.000Z",
          |        "effective_end_date": null
          |      },
          |      "relationships": {
          |        "measure_type": {
          |          "data": {
          |            "id": "714",
          |            "type": "measure_type"
          |          }
          |        },
          |        "footnotes": {
          |           "data": [
          |             {
          |              "id": "CD437",
          |              "type": "footnote"
          |             }
          |           ]
          |        }
          |      }
          |    },
          |    {
          |      "id": "714",
          |      "type": "measure_type",
          |      "attributes": {
          |        "description": "Restriction on entry into free circulation",
          |        "measure_type_series_description": "Entry into free circulation or exportation subject to conditions",
          |        "validity_start_date": "1972-01-01T00:00:00.000Z",
          |        "validity_end_date": null,
          |        "measure_type_series_id": "B",
          |        "trade_movement_code": 0
          |      }
          |    },
          |    {
          |      "id": "CD437",
          |      "type": "footnote",
          |      "attributes": {
          |        "code": "CD437",
          |        "description": "example description"
          |      }
          |    },
          |    {
          |      "id": "D005",
          |      "type": "certificate",
          |      "attributes": {
          |        "certificate_type_code": "D",
          |        "certificate_code": "005",
          |        "code": "D005",
          |        "description": "some certificate description",
          |        "formatted_description": "some certificate description"
          |      }
          |    },
          |    {
          |      "id": "3200",
          |      "type": "additional_code",
          |      "attributes": {
          |        "additional_code_type_id": "3",
          |        "additional_code": "200",
          |        "code": "3200",
          |        "description": "some additional code description",
          |        "formatted_description": "some additional code description"
          |      }
          |    }
          |  ]
          |}
          |""".stripMargin

      val goodsNomenclature = OttJsonApiParser.parse(ottResponse)
      val categoryAssessment = goodsNomenclature.applicable_category_assessments.stream().findFirst().get()
      val measure = categoryAssessment.measures.stream().findFirst().get()
      val measureType = measure.measure_type
      val footnote = measure.footnotes.stream().findFirst().get()
      val certificate: Exemption = categoryAssessment.exemptions.stream().findFirst().get()
      val additionalCode: Exemption = categoryAssessment.exemptions.stream().skip(1).findFirst().get()

      goodsNomenclature.id shouldEqual "106662"
      goodsNomenclature.goods_nomenclature_item_id shouldEqual "2404120000"
      goodsNomenclature.applicable_category_assessments.size() shouldEqual 1

      categoryAssessment.id shouldEqual "123456cd"
      categoryAssessment.measures.size() shouldEqual 1
      categoryAssessment.exemptions.size() shouldEqual 2

      measure.id shouldEqual "3871194"
      measure.goods_nomenclature_sid shouldEqual "106662"
      measure.footnotes.size() shouldEqual 1

      measureType.id shouldEqual "714"
      measureType.measure_type_series_id

      footnote.id shouldEqual "CD437"
      footnote.description shouldEqual "example description"

      certificate.getClass shouldEqual classOf[Certificate]
      certificate.asInstanceOf[Certificate].id shouldEqual "D005"
      certificate.asInstanceOf[Certificate].description shouldEqual "some certificate description"

      additionalCode.getClass shouldEqual classOf[AdditionalCode]
      additionalCode.asInstanceOf[AdditionalCode].id shouldEqual "3200"
      additionalCode.asInstanceOf[AdditionalCode].description shouldEqual "some additional code description"

    }

  }

}