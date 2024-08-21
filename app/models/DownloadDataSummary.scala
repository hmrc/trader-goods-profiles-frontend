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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}

sealed abstract class DownloadDataStatus(val value: String)

object DownloadDataStatus {
  final case object RequestFile extends DownloadDataStatus("RequestFile")

  final case object FileInProgress extends DownloadDataStatus("FileInProgress")

  final case object FileReady extends DownloadDataStatus("FileReady")

  lazy val downloadDataStatusTypes: Set[DownloadDataStatus] = Set(RequestFile, FileInProgress, FileReady)

  private def enumFormat[A](values: Set[A])(getKey: A => String): Format[A] = new Format[A] {

    override def writes(a: A): JsValue =
      JsString(getKey(a))

    override def reads(json: JsValue): JsResult[A] = json match {
      case JsString(str) =>
        values
          .find(getKey(_) == str)
          .map(JsSuccess(_))
          .getOrElse(JsError("error.expected.validenumvalue"))
      case _             =>
        JsError("error.expected.enumstring")
    }
  }

  implicit val format: Format[DownloadDataStatus] =
    enumFormat(DownloadDataStatus.downloadDataStatusTypes)(_.value)
}

final case class DownloadDataSummary(
  eori: String,
  status: DownloadDataStatus
)

object DownloadDataSummary {
  implicit val format: OFormat[DownloadDataSummary] = Json.format[DownloadDataSummary]
}
