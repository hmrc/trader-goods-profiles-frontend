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

package models.helpandsupport

sealed trait HelpAndSupportLink {
  val link: String
  val linkTextKey: String
}

object HelpAndSupportLink {

  case object ImportGoodsIntoUK extends HelpAndSupportLink {
    val link: String        = "https://www.gov.uk/import-goods-into-uk"
    val linkTextKey: String = "helpAndSupport.p2.linkText"
  }

  case object TradingNI extends HelpAndSupportLink {
    val link: String        = "https://www.gov.uk/guidance/trading-and-moving-goods-in-and-out-of-northern-ireland"
    val linkTextKey: String = "helpAndSupport.p3.linkText"
  }

  case object GoodsNotAtRisk extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/check-if-you-can-declare-goods-you-bring-into-northern-ireland-not-at-risk-of-moving-to-the-eu"
    val linkTextKey: String = "helpAndSupport.p4.linkText"
  }

  case object HMRCContact extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/customs-international-trade-and-excise-enquiries"
    val linkTextKey: String = "helpAndSupport.p5.linkText"
  }

  case object FindingCommodityCodes extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/finding-commodity-codes-for-imports-or-exports?step-by-step-nav=849f71d1-f290-4a8e-9458-add936efefc5"
    val linkTextKey: String = "helpAndSupport.p6.linkText"
  }

  case object TradeTariffXI extends HelpAndSupportLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
    val linkTextKey: String = "helpAndSupport.p7.linkText"
  }

  case object TradeTariffHelp extends HelpAndSupportLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/help"
    val linkTextKey: String = "helpAndSupport.p8.linkText"
  }

  case object BindingTariff extends HelpAndSupportLink {
    val link: String        = "https://www.gov.uk/guidance/apply-for-a-binding-tariff-information-decision"
    val linkTextKey: String = "helpAndSupport.p9.linkText"
  }

  case object AskHMRCChat extends HelpAndSupportLink {
    val link: String        = "https://www.tax.service.gov.uk/ask-hmrc/chat/trade-tariff"
    val linkTextKey: String = "helpAndSupport.p10.linkText"
  }

  case object RetailMovementScheme extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/government/publications/retail-movement-scheme-how-the-scheme-will-work/retail-movement-scheme-how-the-scheme-will-work"
    val linkTextKey: String = "helpAndSupport.p11.linkText"
  }

  case object RegisterAndSeal extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/northern-ireland-retail-movement-scheme-how-to-register-and-seal-consignments"
    val linkTextKey: String = "helpAndSupport.p12.linkText"
  }

  case object MovingPlantsGBtoNI extends HelpAndSupportLink {
    val link: String        = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
    val linkTextKey: String = "helpAndSupport.p13.linkText"
  }

  case object AdditionalSupportContacts extends HelpAndSupportLink {
    val link: String        = "#" // TODO add correct link when defined by design
    val linkTextKey: String = "helpAndSupport.p14.linkText"
  }

  case object TraderSupportService extends HelpAndSupportLink {
    val link: String        = "https://www.gov.uk/guidance/trader-support-service"
    val linkTextKey: String = "helpAndSupport.p15.linkText"
  }

  val allLinks: Seq[HelpAndSupportLink] = Seq(
    ImportGoodsIntoUK,
    TradingNI,
    GoodsNotAtRisk,
    HMRCContact,
    FindingCommodityCodes,
    TradeTariffXI,
    TradeTariffHelp,
    BindingTariff,
    AskHMRCChat,
    RetailMovementScheme,
    RegisterAndSeal,
    MovingPlantsGBtoNI,
    AdditionalSupportContacts,
    TraderSupportService
  )

}