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

package models.ott.util

import models.ott.{AdditionalCode, CategoryAssessment, Certificate, Exemption, GoodsNomenclature}
import models.ott.util.OttJsonApiParser
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util
import java.util.Collections
import scala.io.Source

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
      val ottResponse = Source.fromFile("test/models/util/ott-test-response.json", "utf-8").getLines.mkString
      val goodsNomenclature = OttJsonApiParser.parse(ottResponse)
      val categoryAssessment = goodsNomenclature.getCategoryAssessments()(0)
      val measure = categoryAssessment.getMeasures()(0)
      val measureType = measure.measure_type
      val footnote = measure.getFootnotes()(0)
      val certificate: Exemption = categoryAssessment.getExemptions()(0)
      val additionalCode: Exemption = categoryAssessment.getExemptions()(1)

      goodsNomenclature.id shouldEqual "106662"
      goodsNomenclature.goods_nomenclature_item_id shouldEqual "2404120000"
      goodsNomenclature.getCategoryAssessments.size shouldEqual 1

      categoryAssessment.id shouldEqual "123456cd"
      categoryAssessment.getMeasures.size shouldEqual 1
      categoryAssessment.getExemptions.size shouldEqual 2

      measure.id shouldEqual "3871194"
      measure.goods_nomenclature_sid shouldEqual "106662"
      measure.getFootnotes.size shouldEqual 1

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