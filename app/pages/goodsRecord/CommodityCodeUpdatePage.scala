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

package pages.goodsRecord

import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

case class CommodityCodeUpdatePage(recordId: String) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString \ recordId

  override def toString: String = "commodityCodeUpdate"

  override def cleanup(
    newValue: Option[String],
    updatedUserAnswers: UserAnswers,
    originalUserAnswers: UserAnswers
  ): Try[UserAnswers] =
    originalUserAnswers.get(CommodityCodeUpdatePage(recordId)) match {
      case originalValue if originalValue == newValue =>
        Success(updatedUserAnswers)
      case _                                          =>
        updatedUserAnswers.remove(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
    }
}