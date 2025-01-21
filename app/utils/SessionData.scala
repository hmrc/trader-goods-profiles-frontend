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

import utils.Constants._

case object SessionData {
  val dataUpdated: String                    = "changesMade"
  val dataRemoved: String                    = "valueRemoved"
  val dataAdded: String                      = "valueAdded"
  val pageUpdated: String                    = "changedPage"
  val productReference: String               = "productReference"
  val goodsDescription: String               = "goodsDescription"
  val countryOfOrigin: String                = "countryOfOrigin"
  val commodityCode: String                  = "commodityCode"
  val supplementaryUnit: String              = "supplementaryUnit"
  val ukimsNumberPage: String                = ukimsNumberKey
  val newUkimsNumberPage: String             = newUkimsNumberKey
  val hasNirmsPage: String                   = hasNirmsKey
  val hasNiphlPage: String                   = hasNiphlKey
  val nirmsNumberPage: String                = nirmsNumberKey
  val niphlNumberPage: String                = niphlNumberKey
  val ukimsNumberUpdatePage: String          = "UKIMS"
  val hasNirmsUpdatePage: String             = "NIRMS"
  val hasNiphlUpdatePage: String             = "NIPHL"
  val nirmsNumberUpdatePage: String          = "NIRMS"
  val niphlNumberUpdatePage: String          = "NIPHL"
  val productReferencePage: String           = productReferenceKey
  val goodsDescriptionPage: String           = goodsDescriptionKey
  val countryOfOriginPage: String            = countryOfOriginKey
  val commodityCodePage: String              = commodityCodeKey
  val hasCorrectGoodsPage: String            = "hasCorrectGoods"
  val assessmentsPage: String                = "assessments"
  val reassessmentPage: String               = "reassessments"
  val hasSupplementaryUnitPage: String       = "hasSupplementaryUnit"
  val supplementaryUnitPage: String          = "supplementaryUnit"
  val hasSupplementaryUnitUpdatePage: String = "hasSupplementaryUnitUpdate"
  val supplementaryUnitUpdatePage: String    = "supplementaryUnitUpdate"
  val measurementUnitQuery: String           = "measurementUnit"
  val longerCommodityCodePage: String        = "longerCommodityCode"
  val namePage: String                       = "name"
  val emailPage: String                      = "email"
  val initialValueOfHasSuppUnit: String      = "initialValueOfHasSuppUnit"
  val initialValueOfSuppUnit: String         = "initialValueOfSuppUnit"
  val categorisationDetailsQuery             = "categorisationDetails"
  val longerCategorisationDetailsQuery       = "longerCategorisationDetails"
  val longerCommodityQuery                   = "longerCommodity"
  val withDrawAdviceStartPage: String        = "withdrawAdviceStart"
  val reasonForWithdrawAdvicePage: String    = "reasonForWithdrawAdvice"
  val fromExpiredCommodityCodePage: String   = "fromExpiredCommodityCodePage"
  val useExistingUkimsNumberPage: String     = "useExistingUkimsNumberPage"
  val historicProfileDataQuery: String       = "historicProfileData"
  val hasLongComCodeQuery: String            = "hasLongComCode"
}
