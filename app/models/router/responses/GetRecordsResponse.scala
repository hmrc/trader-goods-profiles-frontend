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

import models.GoodsRecordsPagination
import play.api.libs.json._

case class GetRecordsResponse(
  goodsItemRecords: Seq[GetGoodsRecordResponse],
  pagination: GoodsRecordsPagination
)

object GetRecordsResponse {
  implicit val recordsReads: Reads[GetRecordsResponse] = (json: JsValue) =>
    JsSuccess(
      GetRecordsResponse(
        (json \ "goodsItemRecords").as[Seq[GetGoodsRecordResponse]],
        (json \ "pagination").as[GoodsRecordsPagination]
      )
    )

  implicit val recordsWrites: Writes[GetRecordsResponse] = (getRecordsResponse: GetRecordsResponse) =>
    Json.obj(
      "goodsItemRecords" -> getRecordsResponse.goodsItemRecords,
      "pagination"       -> getRecordsResponse.pagination
    )
}
