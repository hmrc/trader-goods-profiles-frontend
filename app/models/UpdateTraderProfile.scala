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
import cats.implicits.catsSyntaxTuple2Parallel
import pages.{HasNiphlUpdatePage, HasNirmsUpdatePage, NiphlNumberUpdatePage, NirmsNumberUpdatePage, UkimsNumberUpdatePage}
import play.api.libs.json.{Json, OFormat}

final case class UpdateTraderProfile(
  actorId: String,
  ukimsNumber: Option[String] = None,
  nirmsNumber: Option[String] = None,
  niphlNumber: Option[String] = None
)

object UpdateTraderProfile {

  implicit lazy val format: OFormat[UpdateTraderProfile] = Json.format

  def buildUkimsNumber(answers: UserAnswers, eori: String): EitherNec[ValidationError, UpdateTraderProfile] =
    (
      Right(eori),
      answers.getPageValue(UkimsNumberUpdatePage)
    ).parMapN((eori, ukimsNumber) =>
      UpdateTraderProfile(
        eori,
        ukimsNumber = Some(ukimsNumber)
      )
    )

  def buildNiphlNumber(answers: UserAnswers, eori: String): EitherNec[ValidationError, UpdateTraderProfile] =
    (
      Right(eori),
      answers.getOptionalPageValue(answers, HasNiphlUpdatePage, NiphlNumberUpdatePage)
    ).parMapN((eori, niphlNumberOpt) =>
      UpdateTraderProfile(
        eori,
        niphlNumber = niphlNumberOpt
      )
    )

  def buildNirmsNumber(answers: UserAnswers, eori: String): EitherNec[ValidationError, UpdateTraderProfile] =
    (
      Right(eori),
      answers.getOptionalPageValue(answers, HasNirmsUpdatePage, NirmsNumberUpdatePage)
    ).parMapN((eori, nirmsNumberOpt) =>
      UpdateTraderProfile(
        eori,
        nirmsNumber = nirmsNumberOpt
      )
    )
}
