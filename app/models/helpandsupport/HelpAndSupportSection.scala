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

case class HelpAndSupportSection(
                                  headingKey: String,
                                  subHeadingKey: Option[String],
                                  paragraphTextKey: Option[String],
                                  linksUnderHeading: Seq[HelpAndSupportLink],
                                  linksUnderSubHeading: Seq[HelpAndSupportLink]
                                )

object HelpAndSupportSection {

  val helpAndSupportSections: Seq[HelpAndSupportSection] = Seq(
    HelpAndSupportSection(
      headingKey = "helpAndSupport.h2.1",
      subHeadingKey = Some("helpAndSupport.h3"),
      paragraphTextKey = None,
      linksUnderHeading = Seq(
        HelpAndSupportLink.ImportGoodsIntoUK,
        HelpAndSupportLink.TradingNI,
        HelpAndSupportLink.GoodsNotAtRisk
      ),
      linksUnderSubHeading = Seq(
        HelpAndSupportLink.HMRCContact
      )
    ),

    HelpAndSupportSection(
      headingKey = "helpAndSupport.h2.2",
      subHeadingKey = Some("helpAndSupport.h3"),
      paragraphTextKey = None,
      linksUnderHeading = Seq(
        HelpAndSupportLink.FindingCommodityCodes,
        HelpAndSupportLink.TradeTariffXI,
        HelpAndSupportLink.TradeTariffHelp,
        HelpAndSupportLink.BindingTariff
      ),
      linksUnderSubHeading = Seq(
        HelpAndSupportLink.AskHMRCChat
      )
    ),

    HelpAndSupportSection(
      headingKey = "helpAndSupport.h2.3",
      subHeadingKey = None,
      paragraphTextKey = None,
      linksUnderHeading = Seq(
        HelpAndSupportLink.RetailMovementScheme,
        HelpAndSupportLink.RegisterAndSeal,
        HelpAndSupportLink.MovingPlantsGBtoNI
      ),
      linksUnderSubHeading = Seq.empty
    ),

    HelpAndSupportSection(
      headingKey = "helpAndSupport.h2.4",
      subHeadingKey = None,
      paragraphTextKey = None,
      linksUnderHeading = Seq(
        HelpAndSupportLink.AdditionalSupportContacts
      ),
      linksUnderSubHeading = Seq.empty
    ),

    HelpAndSupportSection(
      headingKey = "helpAndSupport.h2.5",
      subHeadingKey = None,
      paragraphTextKey = Some("helpAndSupport.p15"),
      linksUnderHeading = Seq(
        HelpAndSupportLink.TraderSupportService
      ),
      linksUnderSubHeading = Seq.empty
    )
  )}
