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

package pages

import models.UserAnswers
import play.api.libs.json.JsPath
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}

import scala.util.{Success, Try}

case class LongerCommodityCodePage(recordId: String) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString \ recordId

  override def toString: String = "longerCommodityCode"

  override def cleanup(
    value: Option[String],
    updatedUserAnswers: UserAnswers,
    originalUserAnswers: UserAnswers
  ): Try[UserAnswers] = {
    val result = for {
      longerComCode <- updatedUserAnswers.get(LongerCommodityCodePage(recordId))
      commodity     <- updatedUserAnswers.get(LongerCommodityQuery(recordId))
      shortCatInfo  <- updatedUserAnswers.get(CategorisationDetailsQuery(recordId))
    } yield
      if (s"${shortCatInfo.commodityCode}$longerComCode" == commodity.commodityCode) {
        super.cleanup(value, updatedUserAnswers, originalUserAnswers)
      } else {
        updatedUserAnswers.remove(HasCorrectGoodsLongerCommodityCodePage(recordId))
      }
    result.getOrElse {
      updatedUserAnswers.remove(HasCorrectGoodsLongerCommodityCodePage(recordId))
    }
  }
}
