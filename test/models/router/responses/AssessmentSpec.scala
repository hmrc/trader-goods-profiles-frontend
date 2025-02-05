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

package models.router.responses

import base.SpecBase
import play.api.libs.json.*

class AssessmentSpec extends SpecBase {

  private val condition: Condition   =
    Condition(Some("type"), Some("conditionId"), Some("conditionDescription"), Some("conditionTraderText"))
  private val assessment: Assessment = Assessment(Some("1234"), Some(100), Some(condition))

  private val conditionJson: JsObject  =
    Json.obj(
      "type"                 -> "type",
      "conditionId"          -> "conditionId",
      "conditionDescription" -> "conditionDescription",
      "conditionTraderText"  -> "conditionTraderText"
    )
  private val assessmentJson: JsObject =
    Json.obj("assessmentId" -> "1234", "primaryCategory" -> 100, "condition" -> conditionJson)

  "Assessment" - {
    "must deserialize from json" in {
      Json.fromJson[Assessment](assessmentJson) mustBe JsSuccess(assessment)
    }

    "must serialize to json" in {
      Json.toJson(assessment) mustBe assessmentJson
    }
  }

}
