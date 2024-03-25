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

import java.time.Instant
import scala.io.Source

class OttResponseSpec extends AnyFreeSpec {
  val jsonFilePath = "test/models/test_data/ott_response_test_data.json"
  val jsonDataString = Source.fromFile(jsonFilePath).mkString
  val jsonData = Json.parse(jsonDataString)

  "OttResponse" - {
    "getApplicableCategoryAssessments" - {
      "should return a list of CategoryAssessment objects" in {
        jsonData.validate[OttResponse] match {
          case JsSuccess(ottResponse, _) =>
            val result = ottResponse.getApplicableCategoryAssessments()

            result should have size 1

            result.map { categoryAssessment =>

              categoryAssessment.id shouldBe Some("abcdef01")
              categoryAssessment.category shouldBe Some("Dummy Category")

              categoryAssessment.exemptions shouldBe Some(
                List(
                  Right(
                    AdditionalCode(
                      Some("9999"),
                      Some("9"),
                      Some("999"),
                      Some("9999"),
                      Some("Dummy additional code description"),
                      Some("Dummy formatted additional code description"))
                  ),
                  Left(
                    Certificate(
                      Some("X999"),
                      Some("X"),
                      Some("999"),
                      Some("X999"),
                      Some("Dummy certificate description"),
                      Some("Dummy formatted certificate description")
                    )
                  )
                )
              )

              categoryAssessment.measures shouldBe Some(
                List(
                  Measure(
                    Some("1234567"),
                    Some("9999990000"),
                    Some("999999"),
                    Some(Instant.parse("2022-01-01T00:00:00Z")),
                    None,
                    Some(MeasureType(
                      Some("789"),
                      Some("Dummy measure description"),
                      Some("Dummy measure series description"),
                      Some(Instant.parse("1972-01-01T00:00:00Z")),
                      None,
                      Some("Z"),
                      Some(0))
                    )
                  )
                )
              )

            }
          case JsError(errors) =>
            fail(s"Failed to validate OttResponse: ${errors.mkString(", ")}")
        }
      }
    }
  }
}