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

import java.time.Instant

final case class GetGoodsRecordResponse(
  recordId: String,
  commodityCode: String,
  countryOfOrigin: String,
  traderRef: String,
  goodsDescription: String,
  declarable: String,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object GetGoodsRecordResponse {

  implicit val reads: Reads[GetGoodsRecordResponse] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "recordId").read[String] and
        (__ \ "comcode").read[String] and
        (__ \ "countryOfOrigin").read[String] and
        (__ \ "traderRef").read[String] and
        (__ \ "goodsDescription").read[String] and
        (__ \ "declarable").read[String] and
        (__ \ "createdDateTime").read[Instant] and
        (__ \ "updatedDateTime").read[Instant]
    )(GetGoodsRecordResponse.apply _)
  }

  implicit val writes: OWrites[GetGoodsRecordResponse] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "recordId").write[String] and
        (__ \ "comcode").write[String] and
        (__ \ "countryOfOrigin").write[String] and
        (__ \ "traderRef").write[String] and
        (__ \ "goodsDescription").write[String] and
        (__ \ "declarable").write[String] and
        (__ \ "createdDateTime").write[Instant] and
        (__ \ "updatedDateTime").write[Instant]
    )(unlift(GetGoodsRecordResponse.unapply))
  }
}
