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

case object SessionData {
  val dataUpdated: String                    = "changesMade"
  val dataRemoved: String                    = "valueRemoved"
  val pageUpdated: String                    = "changedPage"
  val traderReference: String                = "trader reference"
  val goodsDescription: String               = "goods description"
  val countryOfOrigin: String                = "country of origin"
  val commodityCode: String                  = "commodity code"
  val supplementaryUnit: String              = "supplementary unit"
  val ukimsNumberPage: String                = "ukimsNumber"
  val hasNirmsPage: String                   = "hasNirms"
  val hasNiphlPage: String                   = "hasNiphl"
  val nirmsNumberPage: String                = "nirmsNumber"
  val niphlNumberPage: String                = "niphlNumber"
  val traderReferencePage: String            = "traderReference"
  val useTraderReferencePage: String         = "useTraderReference"
  val goodsDescriptionPage: String           = "goodsDescription"
  val countryOfOriginPage: String            = "countryOfOrigin"
  val commodityCodePage: String              = "commodityCode"
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
  val useExistingUkimsPage: String           = "useExistingUkimsPage"
  val historicProfileDataQuery: String       = "historicProfileData"

}
