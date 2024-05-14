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

import cats.data.{EitherNec, Ior, IorNec, NonEmptyChain}
import cats.implicits._
import pages.{HasNiphlPage, HasNirmsPage, NiphlNumberPage, NirmsNumberPage, UkimsNumberPage}
import play.api.libs.json.{Json, OFormat}

final case class TraderProfile(
                                ukimsNumber: String,
                                nirmsNumber: Option[String],
                                niphlNumber: Option[String]
                              )

object TraderProfile {

  implicit lazy val format: OFormat[TraderProfile] = Json.format

  def build(answers: UserAnswers): EitherNec[ValidationError, TraderProfile] =
    (
      answers.getEither(UkimsNumberPage),
      getNirms(answers),
      getNiphl(answers)
    ).parMapN(TraderProfile.apply)

  private def getNirms(answers: UserAnswers): EitherNec[ValidationError, Option[String]] =
    answers.getEither(HasNirmsPage) match {
      case Right(true) => answers.getEither(NirmsNumberPage).map(Some(_))
      case Right(false) => if(answers.isDefined(NirmsNumberPage)) Left(NonEmptyChain.one(UnexpectedPage(NirmsNumberPage))) else Right(None)
      case Left(errors) => Left(errors)
    }

  private def getNiphl(answers: UserAnswers): EitherNec[ValidationError, Option[String]] =
    answers.getEither(HasNiphlPage) match {
      case Right(true)  => answers.getEither(NiphlNumberPage).map(Some(_))
      case Right(false)  => if (answers.isDefined(NiphlNumberPage)) Left(NonEmptyChain.one(UnexpectedPage(NiphlNumberPage))) else Right(None)
      case Left(errors) =>  Left(errors)
    }
}
