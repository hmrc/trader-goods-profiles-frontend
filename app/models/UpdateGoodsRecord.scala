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

import cats.data.{EitherNec}
import cats.implicits.catsSyntaxTuple3Parallel
import models.PageUpdate.getPage
import pages.{CommodityCodeUpdatePage}
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

  def build(
    answers: UserAnswers,
    eori: String,
    recordId: String,
    pageUpdate: PageUpdate
  ): EitherNec[ValidationError, UpdateGoodsRecord] =
    (
      Right(eori),
      Right(recordId),
      pageUpdate match {
        case CommodityCodePageUpdate => answers.getPageValue(CommodityCodeUpdatePage(recordId))
        case _                       => answers.getPageValue(getPage(pageUpdate, recordId))
      }
    ).parMapN((eori, recordId, value) =>
      pageUpdate match {
        case CountryOfOriginPageUpdate  =>
          UpdateGoodsRecord(
            eori,
            recordId,
            countryOfOrigin = Some(value)
          )
        case GoodsDescriptionPageUpdate =>
          UpdateGoodsRecord(
            eori,
            recordId,
            goodsDescription = Some(value)
          )
        case TraderReferencePageUpdate  =>
          UpdateGoodsRecord(
            eori,
            recordId,
            traderReference = Some(value)
          )
        case CommodityCodePageUpdate    =>
          UpdateGoodsRecord(
            eori,
            recordId,
            commodityCode = Some(value)
          )
      }
    )

//  private def getCommodityCode(answers: UserAnswers, recordId: String): EitherNec[ValidationError, String] =
//    answers.getPageValue(CommodityCodeUpdatePage(recordId)) match {
//      case Right(code)  =>
//        answers.getPageValue(HasCorrectGoodsPage(recordId)) match {
//          case Right(true)  => Right(code)
//          case Right(false) => Left(NonEmptyChain.one(UnexpectedPage(HasCorrectGoodsPage(recordId))))
//          case Left(errors) => Left(errors)
//        }
//      case Left(errors) => Left(errors)
//    }
}
