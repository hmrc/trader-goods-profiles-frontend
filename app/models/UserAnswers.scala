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
import pages.QuestionPage
import play.api.libs.json._
import queries.{Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
  id: String,
  data: JsObject = Json.obj(),
  lastUpdated: Instant = Instant.now
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def getPageValue[A](page: Gettable[A])(implicit rds: Reads[A]): EitherNec[ValidationError, A] =
    get(page).map(Right(_)).getOrElse(Left(NonEmptyChain.one(PageMissing(page))))

  def getOptionalPageValue[A](
    answers: UserAnswers,
    questionPage: QuestionPage[Boolean],
    optionalPage: QuestionPage[A]
  )(implicit rds: Reads[A]): EitherNec[ValidationError, Option[A]] =
    getPageValue(questionPage) match {
      case Right(true)  => getPageValue(optionalPage).map(Some(_))
      case Right(false) => unexpectedValueDefined(answers, optionalPage)
      case Left(errors) => Left(errors)
    }

  def getOptionalPageValueForOptionalBooleanPage[A](
    answers: UserAnswers,
    questionPage: QuestionPage[Boolean],
    optionalPage: QuestionPage[A]
  )(implicit rds: Reads[A]): EitherNec[ValidationError, Option[A]] =
    getPageValue(questionPage) match {
      case Right(true) => getPageValue(optionalPage).map(Some(_))
      case _           => unexpectedValueDefined(answers, optionalPage)
    }

  def unexpectedValueDefined(answers: UserAnswers, page: Gettable[_]): EitherNec[ValidationError, Option[Nothing]] =
    if (answers.isDefined(page)) Left(NonEmptyChain.one(UnexpectedPage(page))) else Right(None)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(Some(value), updatedAnswers, this)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(None, updatedAnswers, this)
    }
  }

  def isDefined(gettable: Gettable[_]): Boolean =
    Reads
      .optionNoError(Reads.at[JsValue](gettable.path))
      .reads(data)
      .map(_.isDefined)
      .getOrElse(false)

}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(unlift(UserAnswers.unapply))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
