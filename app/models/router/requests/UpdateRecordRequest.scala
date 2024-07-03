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

package models.router.requests

import models.UpdateGoodsRecord
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}

import scala.Function.unlift

case class UpdateRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  countryOfOrigin: Option[String]
)

object UpdateRecordRequest {

  def map(goodsRecord: UpdateGoodsRecord): UpdateRecordRequest =
    UpdateRecordRequest(
      goodsRecord.eori,
      goodsRecord.recordId,
      goodsRecord.eori,
      Some(goodsRecord.countryOfOrigin)
    )

  implicit val reads: Reads[UpdateRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "recordId").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "countryOfOrigin").readNullable[String])(UpdateRecordRequest.apply _)

  implicit lazy val writes: OWrites[UpdateRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "countryOfOrigin").writeNullable[String])(unlift(UpdateRecordRequest.unapply))

}
