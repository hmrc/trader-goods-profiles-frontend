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

package models

import models.ott.{AdditionalCode, Certificate, Measure, MeasureType, OttResponse}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.have
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.nio.file.{Files, Paths}
import java.time.Instant

class OttResponseSpec extends AnyFreeSpec {
  val jsonFilePath = "ott_response_test_data.json"
  val jsonDataString = scala.io.Source.fromFile(jsonFilePath).mkString
  val jsonData = Json.parse(jsonDataString)

  "OttResponse" - {
    "getApplicableCategoryAssessments" - {
      "should return a list of CategoryAssessment objects" in {
        jsonData.validate[OttResponse] match {
          case JsSuccess(ottResponse, _) =>
            val result = ottResponse.getApplicableCategoryAssessments()

            result should have size 1
            result.headOption.foreach { categoryAssessment =>
              categoryAssessment.id shouldBe Some("123456cd")
              categoryAssessment.category shouldBe Some("Category1")
//              categoryAssessment.theme shouldBe Some("Theme1")
//              categoryAssessment.geographicalArea.map(_.id) shouldBe Some("1011")
              categoryAssessment.exemptions shouldBe Some(List(
                Right(
                  AdditionalCode(
                    Some("3200"),
                    Some("3"),
                    Some("200"),
                    Some("3200"),
                    Some("Mixtures of scheduled substances listed in the Annex to Regulation () No 111/2005 that can be used for the illicit manufacture of narcotic drugs or psychotropic substances"),
                    Some("Mixtures of scheduled substances listed in the Annex to Regulation (EC) No 111/2005 that can be used for the illicit manufacture of narcotic drugs or psychotropic substances")
                  )
                ),
                Left(
                  Certificate(
                    Some("Y069"),
                    Some("Y"),
                    Some("069"),
                    Some("Y069"),
                    Some("Goods not consigned from Iran"),
                    Some("Goods not consigned from Iran")
                  )
                )
              )
              )
              categoryAssessment.measures shouldBe Some(List(
                Measure(
                  Some("3871194"),
                  Some("2404120000"),
                  Some("106662"),
                  Some(Instant.parse("2022-01-01T00:00:00Z")),
                  None,
                  Some(MeasureType(
                    Some("475"),
                    Some("Restriction on entry into free circulation"),
                    Some("Entry into free circulation or exportation subject to conditions"),
                    Some(Instant.parse("1972-01-01T00:00:00Z")),
                    None,
                    Some("B"),
                    Some(0))))
              ))
            }
          case JsError(errors) =>
            fail
        }
      }
    }
  }
}
