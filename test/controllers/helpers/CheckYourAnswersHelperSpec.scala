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

package controllers.helpers

import base.SpecBase
import models.{NiphlNumber, NirmsNumber, TraderGoodsProfile, UkimsNumber}
import viewmodels.govuk.SummaryListFluency
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

class CheckYourAnswersHelperSpec extends SpecBase with SummaryListFluency {

  "CheckYourAnswersHelper" - {

    val checkYourAnswersHelper = new CheckYourAnswersHelper()

    "createSummaryList" - {

      "must return empty summary list when blank trader goods profile" in {
        val traderGoodsProfile: TraderGoodsProfile = TraderGoodsProfile()
        val summaryList                            = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe Seq.empty
      }

      "must return full summary list when trader goods profile has all fields" in {
        val expected                               = List(
          SummaryListRow(
            Key(HtmlContent("UKIMS number")),
            Value(HtmlContent("11")),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/ukims-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS registered"), ""),
            Value(HtmlContent("Yes"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS number"), ""),
            Value(HtmlContent("22"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL registered"), ""),
            Value(HtmlContent("Yes"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL number"), ""),
            Value(HtmlContent("33"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          )
        )
        val traderGoodsProfile: TraderGoodsProfile =
          TraderGoodsProfile(
            Some(UkimsNumber("11")),
            Some(true),
            Some(NirmsNumber("22")),
            Some(true),
            Some(NiphlNumber("33"))
          )
        val summaryList                            = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe expected
      }

      "must return valid summary list when trader goods profile has no for NIPHL" in {
        val expected                               = List(
          SummaryListRow(
            Key(HtmlContent("UKIMS number")),
            Value(HtmlContent("11")),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/ukims-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS registered"), ""),
            Value(HtmlContent("Yes"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS number"), ""),
            Value(HtmlContent("22"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL registered"), ""),
            Value(HtmlContent("No"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          )
        )
        val traderGoodsProfile: TraderGoodsProfile =
          TraderGoodsProfile(
            Some(UkimsNumber("11")),
            Some(true),
            Some(NirmsNumber("22")),
            Some(false)
          )
        val summaryList                            = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe expected
      }

      "must return valid summary list when trader goods profile has no for NIRMS" in {
        val expected                               = List(
          SummaryListRow(
            Key(HtmlContent("UKIMS number")),
            Value(HtmlContent("11")),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/ukims-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS registered"), ""),
            Value(HtmlContent("No"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL registered"), ""),
            Value(HtmlContent("Yes"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL number"), ""),
            Value(HtmlContent("33"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          )
        )
        val traderGoodsProfile: TraderGoodsProfile =
          TraderGoodsProfile(
            Some(UkimsNumber("11")),
            Some(false),
            nirmsNumber = None,
            Some(true),
            Some(NiphlNumber("33"))
          )
        val summaryList                            = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe expected
      }

      "must return valid summary list when trader goods profile has 2 nos" in {
        val expected                               = List(
          SummaryListRow(
            Key(HtmlContent("UKIMS number")),
            Value(HtmlContent("11")),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/ukims-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIRMS registered"), ""),
            Value(HtmlContent("No"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/nirms-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          ),
          SummaryListRow(
            Key(HtmlContent("NIPHL registered"), ""),
            Value(HtmlContent("No"), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-question", HtmlContent("Change"), None, "", Map()))
              )
            )
          )
        )
        val traderGoodsProfile: TraderGoodsProfile =
          TraderGoodsProfile(
            Some(UkimsNumber("11")),
            Some(false),
            nirmsNumber = None,
            Some(false),
            niphlNumber = None
          )
        val summaryList                            = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe expected
      }

    }
  }
}
