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

import cats.data.EitherNec
import cats.implicits.catsSyntaxTuple5Parallel
import pages.advice.{EmailPage, NamePage}
import play.api.libs.json.{Json, OFormat}

final case class AdviceRequest(
  eori: String,
  requestorName: String,
  actorId: String,
  recordId: String,
  requestorEmail: String
)

object AdviceRequest {

  implicit lazy val format: OFormat[AdviceRequest] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, AdviceRequest] =
    (
      Right(eori),
      answers.getPageValue(NamePage(recordId)),
      Right(eori),
      Right(recordId),
      answers.getPageValue(EmailPage(recordId))
    ).parMapN(AdviceRequest.apply)
}
