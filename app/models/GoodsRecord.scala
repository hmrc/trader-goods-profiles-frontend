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
import play.api.libs.json.{Json, OFormat}

final case class GoodsRecord(
  actorId: String,
  traderReference: String,
  commodityCode: String,
  countryOfOrigin: String,
  goodsDescription: String
)

object GoodsRecord {

  implicit lazy val format: OFormat[GoodsRecord] = Json.format

  def build(answers: UserAnswers, eori: String): EitherNec[ValidationError, GoodsRecord] =
    (
      Right(eori),
      answers.getPageValue(TraderReferencePage),
      getCommodityCode(answers),
      answers.getPageValue(CountryOfOriginPage),
      getGoodsDescription(answers)
    ).parMapN(GoodsRecord.apply)

  def getGoodsDescription(answers: UserAnswers): EitherNec[ValidationError, String] =
    answers.getOppositeOptionalPageValue(answers, UseTraderReferencePage, GoodsDescriptionPage) match {
      case Right(Some(data)) => Right(data)
      case Right(None)       => answers.getPageValue(TraderReferencePage)
      case Left(errors)      => Left(errors)
    }

  def getCommodityCode(answers: UserAnswers): EitherNec[ValidationError, String] =
    answers.getPageValue(CommodityCodePage) match {
      case Right(data)  =>
        answers.getPageValue(HasCorrectGoodsPage) match {
          case Right(true)  => Right(data)
          case Right(false) => Left(NonEmptyChain.one(UnexpectedPage(HasCorrectGoodsPage)))
          case Left(errors) => Left(errors)
        }
      case Left(errors) => Left(errors)
    }
}
