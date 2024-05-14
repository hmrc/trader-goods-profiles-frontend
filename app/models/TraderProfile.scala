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

import cats.data.{Ior, IorNec}
import cats.implicits._
import pages.{HasNiphlPage, HasNirmsPage, NiphlNumberPage, NirmsNumberPage, UkimsNumberPage}
import play.api.libs.json.{Json, OFormat}
import queries.Query

final case class TraderProfile(
                                actorId: String,
                                ukimsNumber: String,
                                nirmsNumber: Option[String],
                                niphlNumber: Option[String]
                              )

object TraderProfile {

  implicit lazy val format: OFormat[TraderProfile] = Json.format

  def build(answers: UserAnswers, eori: String): IorNec[Query, TraderProfile] =
    (
      Ior.Right(eori),
      answers.getIor(UkimsNumberPage),
      getNirms(answers),
      getNiphl(answers)
    ).parMapN(TraderProfile.apply)

  private def getNirms(answers: UserAnswers): IorNec[Query, Option[String]] =
    answers.getIor(HasNirmsPage).flatMap {
      case true  => answers.getIor(NirmsNumberPage).map(Some(_))
      case false => Ior.Right(None)
    }

  private def getNiphl(answers: UserAnswers): IorNec[Query, Option[String]] =
    answers.getIor(HasNiphlPage).flatMap {
      case true  => answers.getIor(NiphlNumberPage).map(Some(_))
      case false => Ior.Right(None)
    }
}
