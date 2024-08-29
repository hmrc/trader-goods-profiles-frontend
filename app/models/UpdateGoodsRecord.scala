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
import cats.implicits.{catsSyntaxTuple2Parallel, catsSyntaxTuple3Parallel}
import pages._
import play.api.libs.json.{Json, OFormat}
import queries.CommodityUpdateQuery

import java.time.Instant

final case class UpdateGoodsRecord(
  eori: String,
  recordId: String,
  countryOfOrigin: Option[String] = None,
  goodsDescription: Option[String] = None,
  traderReference: Option[String] = None,
  commodityCode: Option[Commodity] = None,
  category: Option[Int] = None,
  commodityCodeStartDate: Option[Instant] = None,
  commodityCodeEndDate: Option[Instant] = None
)

object UpdateGoodsRecord {

  implicit lazy val format: OFormat[UpdateGoodsRecord] = Json.format

  def buildCountryOfOrigin(
    answers: UserAnswers,
    eori: String,
    recordId: String,
    isCategorised: Boolean
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      getCountryOfOrigin(answers, recordId, isCategorised)
    ).parMapN((eori, recordId, value) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        countryOfOrigin = Some(value),
        category = Some(1)
      )
    )

  def validateGoodsDescription(
    answers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, String] =
    (
      Right(recordId),
      answers.getPageValue(GoodsDescriptionUpdatePage(recordId))
    ).parMapN((_, value) => value)

  def validateCommodityCode(
    answers: UserAnswers,
    recordId: String,
    isCategorised: Boolean,
    isCommCodeExpired: Boolean
  ): EitherNec[ValidationError, Commodity] =
    (
      Right(recordId),
      getCommodityCode(answers, recordId, isCategorised, isCommCodeExpired)
    ).parMapN((_, value) => value)

  def validateTraderReference(
    answers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, String] =
    (
      Right(recordId),
      answers.getPageValue(TraderReferenceUpdatePage(recordId))
    ).parMapN((_, value) => value)

  private def getCommodityCode(
    answers: UserAnswers,
    recordId: String,
    isCategorised: Boolean,
    isCommCodeExpired: Boolean
  ): EitherNec[ValidationError, Commodity] =
    (isCategorised && !isCommCodeExpired, answers.getPageValue(HasCommodityCodeChangePage(recordId))) match {
      case (true, Right(true))  => validateAndGetCommodity(answers, recordId)
      case (true, Right(false)) => Left(NonEmptyChain.one(UnexpectedPage(HasCommodityCodeChangePage(recordId))))
      case (true, Left(errors)) => Left(errors)
      case (false, _)           => validateAndGetCommodity(answers, recordId)
    }

  private def validateAndGetCommodity(
    answers: UserAnswers,
    recordId: String
  ): EitherNec[ValidationError, Commodity] =
    answers.getPageValue(CommodityCodeUpdatePage(recordId)) match {
      case Right(code)  =>
        answers.getPageValue(HasCorrectGoodsCommodityCodeUpdatePage(recordId)) match {
          case Right(true)  => getCommodityUpdateQuery(answers, code, recordId)
          case Right(false) => Left(NonEmptyChain.one(UnexpectedPage(HasCorrectGoodsCommodityCodeUpdatePage(recordId))))
          case Left(errors) => Left(errors)
        }
      case Left(errors) => Left(errors)
    }

  private def getCommodityUpdateQuery(
    answers: UserAnswers,
    code: String,
    recordId: String
  ): EitherNec[ValidationError, Commodity] =
    answers.getPageValue(CommodityUpdateQuery(recordId)) match {
      case Right(commodity) if commodity.commodityCode.startsWith(code) => Right(commodity.copy(commodityCode = code))
      case Left(errors)                                                 => Left(errors)
      case _                                                            => Left(NonEmptyChain.one(MismatchedPage(CommodityCodeUpdatePage(recordId))))
    }

  private def getCountryOfOrigin(
    answers: UserAnswers,
    recordId: String,
    isCategorised: Boolean
  ): EitherNec[ValidationError, String] =
    (isCategorised, answers.getPageValue(HasCountryOfOriginChangePage(recordId))) match {
      case (true, Right(true))  => answers.getPageValue(CountryOfOriginUpdatePage(recordId))
      case (true, Right(false)) => Left(NonEmptyChain.one(UnexpectedPage(HasCountryOfOriginChangePage(recordId))))
      case (true, Left(errors)) => Left(errors)
      case (false, _)           => answers.getPageValue(CountryOfOriginUpdatePage(recordId))
    }
}
