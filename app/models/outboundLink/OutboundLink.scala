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

import config.FrontendAppConfig
import controllers.profile.niphl.routes.*
import controllers.profile.nirms.routes.*
import models.Mode
import models.ott.OttCommodityUrl
import play.api.mvc.Call

sealed trait OutboundLink {
  val link: String
  val linkTextKey: String
  val originatingPage: String

  def outboundCall: Call =
    controllers.routes.OutboundController.redirect(link, linkTextKey, originatingPage)
}

sealed trait HelpAndSupportLink extends OutboundLink {
  val originatingPage: String = controllers.routes.HelpAndSupportController.onPageLoad().url
}

sealed trait ProfileSetupLink extends OutboundLink {
  val originatingPage: String = controllers.profile.routes.ProfileSetupController.onPageLoad().url
}

sealed trait AssessmentViewLink extends OutboundLink {
  def originatingPage(mode: Mode, recordId: String, assessmentNumber: Int, isReassessment: Boolean): String = if (
    isReassessment
  ) {
    controllers.categorisation.routes.AssessmentController
      .onPageLoadReassessment(mode, recordId, assessmentNumber)
      .url
  } else {
    controllers.categorisation.routes.AssessmentController.onPageLoad(mode, recordId, assessmentNumber).url
  }
}

object OutboundLink {

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

  case object ExportGoodsOutUK extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/internal-market-movements-from-great-britain-to-northern-ireland"
    val linkTextKey: String = "helpAndSupport.p11.linkText"
  }

  case object GuidanceUKIMSSummary extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/making-an-entry-summary-declaration"
    val linkTextKey: String = "helpAndSupport.p12.linkText"
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

  case object CategoriseGoodsForSimplifiedProcess extends HelpAndSupportLink {
    val link: String        =
      "https://www.gov.uk/guidance/categorising-goods-for-internal-market-movements-from-great-britain-to-northern-ireland"
    val linkTextKey: String = "helpAndSupport.p13.linkText"
  }

  // Country of Origin Page
  case class CountryOfOrigin(mode: Mode, recordId: Option[String]) extends OutboundLink {
    val link: String            = "https://www.gov.uk/guidance/check-your-goods-meet-the-rules-of-origin"
    val linkTextKey: String     = "countryOfOrigin.p2.linkText"
    val originatingPage: String = recordId match {
      case Some(recordId) =>
        controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController.onSubmit(mode, recordId).url
      case _              => controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onSubmit(mode).url
    }
  }

  // Commodity Code Page
  case class FindCommodity(mode: Mode, recordId: Option[String]) extends OutboundLink {
    val link: String            = "https://www.trade-tariff.service.gov.uk/xi/find_commodity"
    val linkTextKey: String     = "commodityCode.p1.linkText"
    val originatingPage: String = recordId match {
      case Some(recordId) =>
        controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onSubmit(mode, recordId).url
      case _              => controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onSubmit(mode).url
    }
  }

  // Longer Commodity Code Page
  case class FindLongCommodity(mode: Mode, recordId: String, commodityCode: String)(implicit
    appConfig: FrontendAppConfig
  ) extends OutboundLink {
    val link: String            = OttCommodityUrl(commodityCode).link
    val linkTextKey: String     = "longerCommodityCode.linkText"
    val originatingPage: String =
      controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(mode, recordId).url
  }

  // Has Correct Goods Page
  case class FindCommodityHasCorrectGoods(mode: Mode, recordId: Option[String], commodityCode: String)(implicit
    frontendAppConfig: FrontendAppConfig
  ) extends OutboundLink {
    val link: String            = OttCommodityUrl(commodityCode).link
    val linkTextKey: String     = "hasCorrectGoods.p2.linkText"
    val originatingPage: String = recordId match {
      case Some(recordId) =>
        controllers.commodityCodeResult.routes.UpdateCommodityCodeResultController.onSubmit(mode, recordId).url
      case _              => controllers.commodityCodeResult.routes.CreateCommodityCodeResultController.onSubmit(mode).url
    }
  }

  // Assessment Page
  case class FindCommodityAssessments(
    mode: Mode,
    recordId: String,
    assessmentNumber: Int,
    isReassessment: Boolean,
    commodityCode: String
  )(implicit appConfig: FrontendAppConfig)
      extends AssessmentViewLink {

    val link: String            = OttCommodityUrl(commodityCode).link
    val linkTextKey: String     = "assessment.linkText"
    val originatingPage: String = originatingPage(mode, recordId, assessmentNumber, isReassessment)
  }

  case class AssessmentDynamicLink(
    link: String,
    mode: Mode,
    recordId: String,
    assessmentNumber: Int,
    isReassessment: Boolean
  ) extends AssessmentViewLink {
    val linkTextKey: String     = "assessment.regulationUrl.linkText"
    val originatingPage: String = originatingPage(mode, recordId, assessmentNumber, isReassessment)
  }

  // Has Nirms Page
  case class RetailMovement(mode: Mode, isCreateJourney: Boolean) extends OutboundLink {
    val link: String            =
      "https://www.gov.uk/government/publications/retail-movement-scheme-how-the-scheme-will-work/retail-movement-scheme-how-the-scheme-will-work#product-eligibility"
    val linkTextKey: String     = "hasNirms.p2.linkText"
    val originatingPage: String = if (isCreateJourney) {
      CreateIsNiphlRegisteredController.onPageLoad(mode).url
    } else {
      UpdateIsNiphlRegisteredController.onSubmit(mode).url
    }
  }

  // Has Niphl Page
  case class MovingPlants(mode: Mode, isCreateJourney: Boolean) extends OutboundLink {
    val link: String            = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
    val linkTextKey: String     = "hasNiphl.p2.linkText"
    val originatingPage: String = if (isCreateJourney) {
      CreateIsNiphlRegisteredController.onPageLoad(mode).url
    } else {
      UpdateIsNiphlRegisteredController.onSubmit(mode).url
    }
  }

  // Profile Setup Page
  case object ApplyForAuth extends ProfileSetupLink {
    val link: String        =
      "https://www.gov.uk/guidance/apply-for-authorisation-for-the-uk-internal-market-scheme-if-you-bring-goods-into-northern-ireland"
    val linkTextKey: String = "profileSetup.p3.linkText"
  }

  case object RetailMovementProfile extends ProfileSetupLink {
    val link: String        =
      "https://www.gov.uk/guidance/northern-ireland-retail-movement-scheme-how-to-register-and-seal-consignments"
    val linkTextKey: String = "profileSetup.p6.linkText"
  }

  case object MovingPlantProfile extends ProfileSetupLink {
    val link: String        = "https://www.gov.uk/guidance/moving-plants-from-great-britain-to-northern-ireland"
    val linkTextKey: String = "profileSetup.p9.linkText"
  }

  // Advice Start Page
  case class ApplyBinding(recordId: String) extends OutboundLink {
    val link: String            = "https://www.gov.uk/guidance/apply-for-a-binding-tariff-information-decision"
    val linkTextKey: String     = "adviceStart.p2.linkText"
    val originatingPage: String = controllers.advice.routes.AdviceStartController.onPageLoad(recordId).url
  }

  case class RevenueAndCustomsAct(recordId: String) extends OutboundLink {
    val link: String            = "https://www.legislation.gov.uk/ukpga/2005/11/contents"
    val linkTextKey: String     = "adviceStart.p7.link"
    val originatingPage: String = controllers.advice.routes.AdviceStartController.onPageLoad(recordId).url
  }
}
