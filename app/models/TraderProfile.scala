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

import cats.data.{EitherNec, NonEmptyChain, RWS}
import cats.implicits._
import pages._
import play.api.libs.json.{Json, OFormat, Reads}
import cats.implicits.{catsSyntaxTuple2Parallel, catsSyntaxTuple3Parallel}

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

  def buildNirms(
    answers: UserAnswers,
    eori: String,
    traderProfile: TraderProfile
  ): EitherNec[ValidationError, TraderProfile] =
    (
      Right(eori),
      Right(traderProfile.ukimsNumber),
      getOptionallyRemovedPage(answers, HasNirmsUpdatePage, RemoveNirmsPage, NirmsNumberUpdatePage),
      Right(traderProfile.niphlNumber)
    ).parMapN(TraderProfile.apply)

  def buildNiphl(
    answers: UserAnswers,
    eori: String,
    traderProfile: TraderProfile
  ): EitherNec[ValidationError, TraderProfile] =
    (
      Right(eori),
      Right(traderProfile.ukimsNumber),
      Right(traderProfile.nirmsNumber),
      getOptionallyRemovedPage(answers, HasNiphlUpdatePage, RemoveNiphlPage, NiphlNumberUpdatePage)
    ).parMapN(TraderProfile.apply)

  private def getOptionallyRemovedPage[A](
    answers: UserAnswers,
    questionPage: QuestionPage[Boolean],
    removePage: QuestionPage[Boolean],
    optionalPage: QuestionPage[A]
  )(implicit rds: Reads[A]): EitherNec[ValidationError, Option[A]] =
    answers.getPageValue(questionPage) match {
      case Right(true)  => answers.getPageValue(optionalPage).map(Some(_))
      case Right(false) =>
        answers.getPageValue(removePage) match {
          case Right(true)  => Right(None)
          case Right(false) => answers.unexpectedValueDefined(answers, removePage)
          case Left(errors) => Left(errors)
        }
      case Left(errors) => Left(errors)
    }

  def buildHasNirms(
    answers: UserAnswers
  ): EitherNec[ValidationError, Boolean] =
    (answers.getPageValue(RemoveNirmsPage), answers.getPageValue(HasNirmsUpdatePage)) match {
      case (Right(true), Right(false)) => answers.getPageValue(HasNirmsUpdatePage).map(value => value)
      case (_, _) => Left(NonEmptyChain.one(UnexpectedPage(HasNirmsUpdatePage)))
    }
}
