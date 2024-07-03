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
import cats.implicits.catsSyntaxTuple3Parallel
import pages.CountryOfOriginPage
import play.api.libs.json.{Json, OFormat}

final case class UpdateGoodsRecord(
  eori: String,
  recordId: String,
  countryOfOrigin: String
)

object UpdateGoodsRecord {

  implicit lazy val format: OFormat[UpdateGoodsRecord] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      answers.getPageValue(CountryOfOriginPage(recordId))
    ).parMapN((eori, recordId, countryOfOrigin) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        countryOfOrigin
      )
    )
}
