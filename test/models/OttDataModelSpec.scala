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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

import models.ott.OttDataModel

class OttDataModelSpec extends AnyFreeSpec with Matchers {

  "OttDataModel" - {

    "should serialize and deserialize properly" in {
      val dataModel = OttDataModel(
        id = "1",
        `type` = "test",
        attributes = Some(Map("key1" -> Json.toJson("value1"))),
        relationships = Some(Map("key2" -> Json.toJson("value2"))),
        links = Some(Map("self" -> "http://example.com")),
        meta = Some(Map("info" -> "test"))
      )

      val jsonString = Json.toJson(dataModel).toString()
      val deserializedModel = Json.parse(jsonString).as[OttDataModel]

      deserializedModel shouldBe dataModel
    }

    "should serialize and deserialize properly without attributes, relationships, links, and meta" in {
      val dataModel = OttDataModel(
        id = "1",
        `type` = "test",
        attributes = None,
        relationships = None,
        links = None,
        meta = None
      )

      val jsonString = Json.toJson(dataModel).toString()
      val deserializedModel = Json.parse(jsonString).as[OttDataModel]

      deserializedModel shouldBe dataModel
    }

    "should fail to deserialize if required fields are missing" in {
      val jsonWithNoId = Json.obj("type" -> "test")
      val jsonWithNoType = Json.obj("id" -> "1")
      val emptyJson = JsObject.empty
      Json.fromJson[OttDataModel](jsonWithNoId).isSuccess shouldBe false
      Json.fromJson[OttDataModel](jsonWithNoType).isSuccess shouldBe false
      Json.fromJson[OttDataModel](emptyJson).isSuccess shouldBe false
    }

  }
}