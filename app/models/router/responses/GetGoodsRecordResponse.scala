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

import play.api.libs.json.{OWrites, Reads, __}

final case class GetGoodsRecordResponse(
  recordId: String,
  commodityCode: String,
  countryOfOrigin: String
)

object GetGoodsRecordResponse {

  implicit val reads: Reads[GetGoodsRecordResponse] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "recordId").read[String] and
        (__ \ "comcode").read[String] and
        (__ \ "countryOfOrigin").read[String]
    )(GetGoodsRecordResponse.apply _)
  }

  implicit val writes: OWrites[GetGoodsRecordResponse] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "recordId").write[String] and
        (__ \ "comcode").write[String] and
        (__ \ "countryOfOrigin").write[String]
    )(unlift(GetGoodsRecordResponse.unapply))
  }
}
