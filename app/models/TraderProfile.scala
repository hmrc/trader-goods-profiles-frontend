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

import cats.data.{EitherNec, NonEmptyChain}
import cats.implicits._
import pages._
import pages.newUkims.NewUkimsNumberPage
import pages.profile.niphl._
import pages.profile.nirms._
import pages.profile.ukims.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.libs.json.{Json, OFormat}
import queries.TraderProfileQuery

final case class TraderProfile(
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  eoriChanged: Boolean
)

object TraderProfile {

  implicit lazy val format: OFormat[TraderProfile] = Json.format

  def build(answers: UserAnswers, eori: String): EitherNec[ValidationError, TraderProfile] =
    (
      Right(eori),
      answers.getPageValue(UkimsNumberPage),
      answers.getOptionalPageValue(answers, HasNirmsPage, NirmsNumberPage),
      answers.getOptionalPageValue(answers, HasNiphlPage, NiphlNumberPage),
      Right(false)
    ).parMapN(TraderProfile.apply)

  private def getOptionallyRemovedPage(
    answers: UserAnswers,
    questionPage: QuestionPage[Boolean],
    removePage: QuestionPage[Boolean],
    optionalPage: QuestionPage[String],
    isRegistered: Boolean
  ): EitherNec[ValidationError, Option[String]] =
    answers.getPageValue(questionPage) match {
      case Right(true)  =>
        if (isRegistered) {
          answers.getPageValue(optionalPage).map(Some(_))
        } else {
          Left(NonEmptyChain.one(IncorrectlyAnsweredPage(questionPage)))
        }
      case Right(false) =>
        if (isRegistered) {
          Left(NonEmptyChain.one(IncorrectlyAnsweredPage(questionPage)))
        } else {
          answers.getPageValue(removePage) match {
            case Right(true)  => Right(None)
            case Right(false) => answers.unexpectedValueDefined(answers, removePage)
            case Left(errors) =>
              removePage match {
                case RemoveNirmsPage if answers.getPageValue(TraderProfileQuery).exists(_.nirmsNumber.isEmpty) =>
                  Right(None)
                case RemoveNiphlPage if answers.getPageValue(TraderProfileQuery).exists(_.niphlNumber.isEmpty) =>
                  Right(None)
                case _                                                                                         => Left(errors)
              }
          }
        }
      case Left(errors) => Left(errors)
    }

  def validateUkimsNumber(
    answers: UserAnswers
  ): EitherNec[ValidationError, String] =
    answers.getPageValue(UkimsNumberUpdatePage)

  def validateNewUkimsNumber(
    answers: UserAnswers
  ): EitherNec[ValidationError, String] =
    answers.getPageValue(NewUkimsNumberPage)

  def validateHasNirms(
    answers: UserAnswers
  ): EitherNec[ValidationError, Option[String]] = getOptionallyRemovedPage(
    answers,
    HasNirmsUpdatePage,
    RemoveNirmsPage,
    NirmsNumberUpdatePage,
    isRegistered = false
  )

  def validateHasNiphl(
    answers: UserAnswers
  ): EitherNec[ValidationError, Option[String]] = getOptionallyRemovedPage(
    answers,
    HasNiphlUpdatePage,
    RemoveNiphlPage,
    NiphlNumberUpdatePage,
    isRegistered = false
  )

  def validateNirmsNumber(
    answers: UserAnswers
  ): EitherNec[ValidationError, Option[String]] = getOptionallyRemovedPage(
    answers,
    HasNirmsUpdatePage,
    RemoveNirmsPage,
    NirmsNumberUpdatePage,
    isRegistered = true
  )

  def validateNiphlNumber(
    answers: UserAnswers
  ): EitherNec[ValidationError, Option[String]] = getOptionallyRemovedPage(
    answers,
    HasNiphlUpdatePage,
    RemoveNiphlPage,
    NiphlNumberUpdatePage,
    isRegistered = true
  )

}
