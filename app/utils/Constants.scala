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

package utils

case object Constants {
  val firstAssessmentNumber: Int = 1
  val firstAssessmentIndex: Int  = 0

  val Category1AsInt: Int     = 1
  val Category2AsInt: Int     = 2
  val StandardGoodsAsInt: Int = 3

  val NiphlCode: String = "WFE012"
  val NirmsCode: String = "WFE013"

  val minimumLengthOfCommodityCode: Int = 6

  val ukimsNumberKey    = "ukimsNumber"
  val newUkimsNumberKey = "newUkimsNumber"
  val hasNirmsKey       = "hasNirms"
  val nirmsNumberKey    = "nirmsNumber"
  val hasNiphlKey       = "hasNiphl"
  val niphlNumberKey    = "niphlNumber"

  val productReferenceKey  = "productReference"
  val goodsDescriptionKey = "goodsDescription"
  val countryOfOriginKey  = "countryOfOrigin"
  val commodityCodeKey    = "commodityCode"

  val maximumEmailLength: Int = 254

  val maxNameLength: Int = 70
}
