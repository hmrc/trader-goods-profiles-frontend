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

package models.outboundLink

sealed trait OutboundLink {
  val link: String
  val linkTextKey: String
  val originatingPage: String
}

object OutboundLink { // TODO: Use case class for the links, but take the link text in have an approved list of message keys for the links?

  // Help & Support Page
  case class ImportGoodsIntoUK(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/import-goods-into-uk"
  }

  case class TradingNI(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/trading-and-moving-goods-in-and-out-of-northern-ireland"
  }

  case class GoodsNotAtRisk(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/guidance/check-if-you-can-declare-goods-you-bring-into-northern-ireland-not-at-risk-of-moving-to-the-eu"
  }

  case class HMRCContact(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/customs-international-trade-and-excise-enquiries"
  }

  case class FindingCommodityCodes(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/guidance/finding-commodity-codes-for-imports-or-exports?step-by-step-nav=849f71d1-f290-4a8e-9458-add936efefc5"
  }

  case class TradeTariffXI(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
  }

  case class TradeTariffHelp(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/help"
  }

  case class BindingTariff(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/apply-for-a-binding-tariff-information-decision"
  }

  case class AskHMRCChat(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.tax.service.gov.uk/ask-hmrc/chat/trade-tariff"
  }

  case class RetailMovementScheme(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/government/publications/retail-movement-scheme-how-the-scheme-will-work/retail-movement-scheme-how-the-scheme-will-work"
  }

  case class RegisterAndSeal(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/guidance/northern-ireland-retail-movement-scheme-how-to-register-and-seal-consignments"
  }

  case class MovingPlantsGBtoNI(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
  }

  case class AdditionalSupportContacts(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "#" // TODO add correct link when defined by design
  }

  case class TraderSupportService(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/trader-support-service"
  }

  // Country of Origin Page
  case class CountryOfOrigin(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/check-your-goods-meet-the-rules-of-origin"
  }

  // Commodity Code Page
  case class FindCommodity(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
  }

  // Longer Commodity Code Page
  case class FindLongCommodity(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
  }

  // Has Correct Goods Page
  case class FindCommodityHasCorrectGoods(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
  }

  // Assessment Page
  case class FindCommodityAssessments(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
  }

  // Has Nirms Page
  case class RetailMovement(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/government/publications/retail-movement-scheme-how-the-scheme-will-work/retail-movement-scheme-how-the-scheme-will-work#product-eligibility"
  }

  // Has Niphl Page
  case class MovingPlants(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
  }

  // Profile Setup Page
  case class ApplyForAuth(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/guidance/apply-for-authorisation-for-the-uk-internal-market-scheme-if-you-bring-goods-into-northern-ireland"
  }

  case class RetailMovementProfile(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        =
      "https://www.gov.uk/guidance/northern-ireland-retail-movement-scheme-how-to-register-and-seal-consignments"
  }

  case class MovingPlantProfile(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
  }

  // Advice Start Page
  case class ApplyBinding(linkTextKey: String, originatingPage: String) extends OutboundLink {
    val link: String        = "https://www.gov.uk/guidance/apply-for-a-binding-tariff-information-decision"
  }
//
//  val allLinks: Seq[OutboundLink] = Seq(
//    ImportGoodsIntoUK,
//    TradingNI,
//    GoodsNotAtRisk,
//    HMRCContact,
//    FindingCommodityCodes,
//    TradeTariffXI,
//    TradeTariffHelp,
//    BindingTariff,
//    AskHMRCChat,
//    RetailMovementScheme,
//    RegisterAndSeal,
//    MovingPlantsGBtoNI,
//    AdditionalSupportContacts,
//    TraderSupportService,
//    CountryOfOrigin,
//    FindCommodity,
//    FindLongCommodity,
//    FindCommodityHasCorrectGoods,
//    FindCommodityAssessments,
//    RetailMovement,
//    MovingPlants,
//    ApplyForAuth,
//    RetailMovementProfile,
//    MovingPlantProfile,
//    ApplyBinding
//  )

}
