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

package models.ott

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsError, JsResult, JsSuccess, JsValue, Json, __}

case class Data(
  id: String,
  `type`: String,
  attributes: Option[Map[String, JsValue]],
  relationships: Option[Map[String, Map[String, Either[Data, List[Data]]]]]
)

object Data {
  implicit lazy val dataFormat: Format[Data] = (
    (__ \ "id").format[String] and
      (__ \ "type").format[String] and
      (__ \ "attributes").formatNullable[Map[String, JsValue]] and
      (__ \ "relationships").formatNullable[Map[String, Map[String, Either[Data, List[Data]]]]]
    )(Data.apply, unlift(Data.unapply))

  implicit val dataEitherFormat: Format[Either[Data, List[Data]]] = new Format[Either[Data, List[Data]]] {
    override def reads(json: JsValue): JsResult[Either[Data, List[Data]]] = {
      json.validate[Data] match {
        case JsSuccess(data, _) => JsSuccess(Left(data))
        case JsError(_) => {
          json.validate[List[Data]] match {
            case JsSuccess(data, _) => JsSuccess(Right(data))
            case JsError(_) => JsError()
          }
        }
      }
    }

    override def writes(o: Either[Data, List[Data]]): JsValue = o match {
      case Left(data) => dataFormat.writes(data)
      case Right(listData) => Json.toJson(listData)
    }
  }
}