package models

import cats.data.{Ior, IorNec}
import cats.implicits._
import pages.{HasNiphlPage, HasNirmsPage, NiphlNumberPage, NirmsNumberPage, UkimsNumberPage}
import queries.Query

final case class TraderProfile(
                                ukimsNumber: String,
                                nirmsNumber: Option[String],
                                niphlNumber: Option[String]
                              )

object TraderProfile {

  def build(answers: UserAnswers): IorNec[Query, TraderProfile] =
    (
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
