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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class GoodsNomenclatureResponseSpec extends AnyFreeSpec with Matchers {

  ".reads" - {

    "must deserialise valid JSON" in {

      val now = Instant.now()

      val json = Json.obj(
        "data"     -> Json.obj(
          "id"         -> "1",
          "attributes" -> Json.obj(
            "goods_nomenclature_item_id" -> "foo",
            "supplementary_measure_unit" -> "bar",
            "validity_start_date"        -> Instant.EPOCH.toString,
            "validity_end_date"          -> now.toString,
            "description"                -> "Other"
          )
        ),
        "included" -> Json.arr(
          Json.obj(
            "id"         -> "38193",
            "type"       -> "goods_nomenclature",
            "attributes" -> Json.obj(
              "description" -> "EXPLOSIVES; PYROTECHNIC PRODUCTS; MATCHES; PYROPHORIC ALLOYS; CERTAIN COMBUSTIBLE PREPARATIONS"
            )
          ),
          Json.obj(
            "id"         -> "38195",
            "type"       -> "goods_nomenclature",
            "attributes" -> Json.obj(
              "description" -> "Prepared explosives, other than propellent powders"
            )
          )
        )
      )

      val result = json.validate[GoodsNomenclatureResponse]
      result mustEqual JsSuccess(
        GoodsNomenclatureResponse(
          "1",
          "foo",
          Some("bar"),
          Instant.EPOCH,
          Some(now),
          List(
            "EXPLOSIVES; PYROTECHNIC PRODUCTS; MATCHES; PYROPHORIC ALLOYS; CERTAIN COMBUSTIBLE PREPARATIONS",
            "Prepared explosives, other than propellent powders",
            "Other"
          )
        )
      )
    }
  }
}
