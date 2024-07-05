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

import models.CategoryRecord
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsPath, OWrites, Reads}

import scala.Function.unlift

case class UpdateCategoryRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  category: Option[Int],
  supplementaryUnit: Option[Double],
  measurementUnit: Option[String]
)

object UpdateCategoryRecordRequest {

  def map(categoryRecord: CategoryRecord): UpdateCategoryRecordRequest =
    UpdateCategoryRecordRequest(
      categoryRecord.eori,
      categoryRecord.recordId,
      categoryRecord.eori,
      Some(categoryRecord.category),
      convertToDouble(categoryRecord.supplementaryUnit),
      categoryRecord.measurementUnit
    )

  implicit val reads: Reads[UpdateCategoryRecordRequest] =
    ((JsPath \ "eori").read[String] and
      (JsPath \ "recordId").read[String] and
      (JsPath \ "actorId").read[String] and
      (JsPath \ "category").readNullable[Int] and
      (JsPath \ "supplementaryUnit").readNullable[Double] and
      (JsPath \ "measurementUnit").readNullable[String])(UpdateCategoryRecordRequest.apply _)

  implicit lazy val writes: OWrites[UpdateCategoryRecordRequest] =
    ((JsPath \ "eori").write[String] and
      (JsPath \ "recordId").write[String] and
      (JsPath \ "actorId").write[String] and
      (JsPath \ "category").writeNullable[Int] and
      (JsPath \ "supplementaryUnit").writeNullable[Double] and
      (JsPath \ "measurementUnit").writeNullable[String])(unlift(UpdateCategoryRecordRequest.unapply))

  private def convertToDouble(value: Option[String]): Option[Double] =
    value.map(_.toDouble)
}
