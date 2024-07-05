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
import pages._
import play.api.libs.json.{Json, OFormat}

final case class UpdateGoodsRecord(
  eori: String,
  recordId: String,
  countryOfOrigin: Option[String] = None,
  goodsDescription: Option[String] = None,
  traderReference: Option[String] = None,
  commodityCode: Option[String] = None
)

object UpdateGoodsRecord {

  implicit lazy val format: OFormat[UpdateGoodsRecord] = Json.format

  def buildCountryOfOrigin(
    answers: UserAnswers,
    eori: String,
    recordId: String
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      answers.getPageValue(CountryOfOriginUpdatePage(recordId))
    ).parMapN((eori, recordId, value) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        countryOfOrigin = Some(value)
      )
    )

  def buildGoodsDescription(
    answers: UserAnswers,
    eori: String,
    recordId: String
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      answers.getPageValue(GoodsDescriptionUpdatePage(recordId))
    ).parMapN((eori, recordId, value) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        goodsDescription = Some(value)
      )
    )

  def buildCommodityCode(
    answers: UserAnswers,
    eori: String,
    recordId: String
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      answers.getPageValue(CommodityCodeUpdatePage(recordId))
    ).parMapN((eori, recordId, value) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        commodityCode = Some(value)
      )
    )

  def buildTraderReference(
    answers: UserAnswers,
    eori: String,
    recordId: String
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      answers.getPageValue(TraderReferenceUpdatePage(recordId))
    ).parMapN((eori, recordId, value) =>
      UpdateGoodsRecord(
        eori,
        recordId,
        traderReference = Some(value)
      )
    )
}
