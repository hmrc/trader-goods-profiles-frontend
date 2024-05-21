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

final case class GoodsRecord(
  actorId: String,
  commodityCode: String,
  countryOfOrigin: String,
  goodsDescription: String
)

object GoodsRecord {

  implicit lazy val format: OFormat[GoodsRecord] = Json.format

  def build(answers: UserAnswers, eori: String): EitherNec[ValidationError, GoodsRecord] =
    (
      Right(eori),
      answers.getPageValue(CommodityCodePage),
      answers.getPageValue(CountryOfOriginPage),
      getGoodsDescription(answers)
    ).parMapN((eori, commodity, country, description) =>
      GoodsRecord(eori, commodity.commodityCode, country, description)
    )

  def getGoodsDescription(answers: UserAnswers): EitherNec[ValidationError, String] =
    answers.getOptionalPageValue(answers, HasGoodsDescriptionPage, GoodsDescriptionPage) match {
      case Right(Some(data)) => Right(data)
      case Right(None)       => answers.getPageValue(CountryOfOriginPage)
      case Left(errors)      => Left(errors)
    }
}
