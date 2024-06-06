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
import models.router.Assessment
import pages._
import play.api.libs.json.{Json, OFormat}
import queries.CommodityQuery

import java.time.Instant

final case class GoodsRecord(
  eori: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  recordId: String = "",
  category: Option[Int] = None,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[Int] = None,
  measurementUnit: Option[String] = None
)

object GoodsRecord {

  implicit lazy val format: OFormat[GoodsRecord] = Json.format

  def build(answers: UserAnswers, eori: String, recordId: String = ""): EitherNec[ValidationError, GoodsRecord] =
    (
      Right(eori),
      answers.getPageValue(TraderReferencePage),
      getCommodity(answers),
      answers.getPageValue(CountryOfOriginPage),
      getGoodsDescription(answers)
    ).parMapN((eori, traderReference, commodity, countryOfOrigin, goodsDescription) =>
      GoodsRecord(
        eori,
        traderReference,
        commodity.commodityCode,
        goodsDescription,
        countryOfOrigin,
        commodity.validityStartDate,
        commodity.validityEndDate,
        recordId
      )
    )

  private def getGoodsDescription(answers: UserAnswers): EitherNec[ValidationError, String] =
    answers.getOppositeOptionalPageValue(answers, UseTraderReferencePage, GoodsDescriptionPage) match {
      case Right(Some(data)) => Right(data)
      case Right(None)       => answers.getPageValue(TraderReferencePage)
      case Left(errors)      => Left(errors)
    }

  private def getCommodity(answers: UserAnswers): EitherNec[ValidationError, Commodity] =
    answers.getPageValue(CommodityCodePage) match {
      case Right(code)  =>
        answers.getPageValue(HasCorrectGoodsPage) match {
          case Right(true)  => getCommodityQuery(answers, code)
          case Right(false) => Left(NonEmptyChain.one(UnexpectedPage(HasCorrectGoodsPage)))
          case Left(errors) => Left(errors)
        }
      case Left(errors) => Left(errors)
    }

  private def getCommodityQuery(answers: UserAnswers, code: String): EitherNec[ValidationError, Commodity] =
    answers.getPageValue(CommodityQuery) match {
      case Right(commodity) if commodity.commodityCode == code => Right(commodity)
      case Left(errors)                                        => Left(errors)
      case _                                                   => Left(NonEmptyChain.one(MismatchedPage(CommodityCodePage)))
    }

}
