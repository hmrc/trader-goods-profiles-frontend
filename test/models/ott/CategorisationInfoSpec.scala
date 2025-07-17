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

import base.SpecBase
import models.TraderProfile
import models.ott.response.*
import org.scalatest.matchers.should.Matchers.{should, shouldBe}

import java.time.Instant

class CategorisationInfoSpec extends SpecBase {

  val testGoodsNomenclature: GoodsNomenclatureResponse = GoodsNomenclatureResponse(
    id = "id1",
    commodityCode = "1234567890",
    measurementUnit = Some("unit"),
    validityStartDate = Instant.parse("2023-01-01T00:00:00Z"),
    validityEndDate = Some(Instant.parse("2024-01-01T00:00:00Z")),
    descriptions = List("Description 1", "Description 2")
  )

  val themeResponse: ThemeResponse = ThemeResponse("theme1", 1, "Theme 1 description")

  val categoryAssessmentResponse: CategoryAssessmentResponse = CategoryAssessmentResponse(
    id = "ca1",
    themeId = "theme1",
    exemptions = Seq.empty,
    regulationId = "reg1"
  )

  val categoryAssessmentRelationship: CategoryAssessmentRelationship = CategoryAssessmentRelationship(id = "car1")

  val ottResponse: OttResponse = OttResponse(
    goodsNomenclature = testGoodsNomenclature,
    categoryAssessmentRelationships = Seq(categoryAssessmentRelationship),
    includedElements = Seq(themeResponse, categoryAssessmentResponse),
    descendents = Seq.empty
  )

  "CategorisationInfo.build" - {
    "build CategorisationInfo correctly" in {
      val traderProfile = TraderProfile("actor1", "ukims", Some("niphl"), Some("nirms"), eoriChanged = false)

      val result = CategorisationInfo.build(ottResponse, "BV", "1234567890", traderProfile)

      result.isRight shouldBe true
      val categorisationInfo = result.toOption.get

      categorisationInfo.commodityCode shouldBe "1234567890"
      categorisationInfo.countryOfOrigin shouldBe "BV"
      categorisationInfo.comcodeEffectiveToDate should contain (Instant.parse("2024-01-01T00:00:00Z"))
      categorisationInfo.measurementUnit should contain ("unit")
      categorisationInfo.isTraderNiphlAuthorised shouldBe true
      categorisationInfo.isTraderNirmsAuthorised shouldBe true
      categorisationInfo.categoryAssessments should not be empty
    }
  }
}
