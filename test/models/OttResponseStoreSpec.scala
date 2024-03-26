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

import models.ott.OttResponseStore
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class OttResponseStoreSpec extends AnyFreeSpec with Matchers {

  val json = Json.parse(
    """
      |{
      |  "data": {
      |    "id": "1",
      |    "type": "root",
      |    "attributes": {},
      |    "relationships": {
      |      "applicable_category_assessments": {
      |        "data": [
      |          {
      |            "id": "2",
      |            "type": "category_assessment"
      |          }
      |        ]
      |      }
      |    }
      |  },
      |  "included": [
      |    {
      |      "id": "2",
      |      "type": "category_assessment",
      |      "attributes": {},
      |      "relationships": {
      |        "measures": {
      |          "data": [
      |            {
      |              "id": "3",
      |              "type": "measure"
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    {
      |      "id": "3",
      |      "type": "measure",
      |      "attributes": {},
      |      "relationships": {}
      |    }
      |  ]
      |}
      |""".stripMargin)

  val ottResponseStore = new OttResponseStore(json)

  "OttResponseStore" - {
    "should correctly retrieve root model" in {
      val root = ottResponseStore.getRoot
      root.id shouldEqual "1"
      root.`type` shouldEqual "root"
    }

    "should correctly retrieve included models" in {
      val included = ottResponseStore.getIncluded
      included.size shouldEqual 2
      included.map(_.id) should contain allOf ("2", "3")
    }

    "should correctly retrieve model by id" in {
      ottResponseStore.getById("2").`type` shouldEqual "category_assessment"
    }

    "should correctly retrieve applicable category assessments" in {
      val assessments = ottResponseStore.getApplicableCategoryAssessments
      assessments.size shouldEqual 1
      assessments(0).id shouldEqual "2"
    }

    "should correctly retrieve measures for assessment" in {
      val assessment = ottResponseStore.getApplicableCategoryAssessments(0)
      val measures = ottResponseStore.getMeasuresForAssessment(assessment)
      measures.size shouldEqual 1
      measures(0).id shouldEqual "3"
      measures(0).`type` shouldEqual "measure"
    }
  }
}

