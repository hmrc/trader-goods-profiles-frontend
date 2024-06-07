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
import pages.{HasSupplementaryUnitPage, SupplementaryUnitPage}
import play.api.libs.json.{Json, OFormat}

final case class CategorisationAnswers (
  assessmentValues: Seq[String],
  supplementaryUnit: Option[Int]
)

object CategorisationAnswers {

  implicit lazy val format: OFormat[CategorisationAnswers] = Json.format

  def build(answers: UserAnswers): EitherNec[ValidationError, CategorisationAnswers] = {
    (
      Right(Seq.empty[String]),
      answers.getOptionalPageValueForOptionalBooleanPage(answers, HasSupplementaryUnitPage, SupplementaryUnitPage)
    ).parMapN(CategorisationAnswers.apply)
  }
}


