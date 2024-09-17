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

package models.helper

import utils.SessionData._

sealed trait Journey

case object CreateProfileJourney extends Journey {
  val pages: Seq[String]        = Seq(
    ukimsNumberPage,
    hasNirmsPage,
    hasNiphlPage,
    nirmsNumberPage,
    niphlNumberPage,
    useExistingUkimsNumberPage,
    historicProfileDataQuery
  )
  override def toString: String = "CreateProfile"
}

case object MaintainProfileJourney extends Journey {
  val pages: Seq[String]        = Seq(
    ukimsNumberUpdatePage,
    hasNirmsUpdatePage,
    hasNiphlUpdatePage,
    nirmsNumberUpdatePage,
    niphlNumberUpdatePage,
    useExistingUkimsPage,
    historicProfileDataQuery
  )
  override def toString: String = "CreateProfile"
}

case object RemoveRecord extends Journey

case object CreateRecordJourney extends Journey {
  val pages: Seq[String]        =
    Seq(
      traderReferencePage,
      useTraderReferencePage,
      goodsDescriptionPage,
      countryOfOriginPage,
      commodityCodePage,
      hasCorrectGoodsPage
    )
  override def toString: String = "CreateRecord"
}
case object UpdateRecordJourney extends Journey {

  override def toString: String = "UpdateRecord"
}

case object CategorisationJourney extends Journey {
  val pages: Seq[String]        =
    Seq(
      assessmentsPage,
      hasSupplementaryUnitPage,
      supplementaryUnitPage,
      longerCommodityCodePage,
      reassessmentPage,
      categorisationDetailsQuery,
      longerCommodityQuery,
      longerCategorisationDetailsQuery
    )
  override def toString: String = "Categorisation"
}
case object RequestAdviceJourney extends Journey {
  val pages: Seq[String]        =
    Seq(
      namePage,
      emailPage
    )
  override def toString: String = "RequestAdvice"
}

case object WithdrawAdviceJourney extends Journey {
  val pages: Seq[String]        =
    Seq(
      withDrawAdviceStartPage,
      reasonForWithdrawAdvicePage
    )
  override def toString: String = "WithdrawAdvice"
}

case object SupplementaryUnitUpdateJourney extends Journey {
  val pages: Seq[String]        =
    Seq(
      hasSupplementaryUnitUpdatePage,
      supplementaryUnitUpdatePage,
      measurementUnitQuery
    )
  override def toString: String = "SupplementaryUnitUpdate"
}
