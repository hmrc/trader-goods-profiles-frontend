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
import cats.implicits._
import pages._
import play.api.libs.json.{Json, OFormat}

final case class TraderProfile(
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object TraderProfile {

  implicit lazy val format: OFormat[TraderProfile] = Json.format

  def build(answers: UserAnswers, eori: String): EitherNec[ValidationError, TraderProfile] =
    (
      Right(eori),
      answers.getPageValue(UkimsNumberPage),
      answers.getOptionalPageValue(answers, HasNirmsPage, NirmsNumberPage),
      answers.getOptionalPageValue(answers, HasNiphlPage, NiphlNumberPage)
    ).parMapN(TraderProfile.apply)
}
