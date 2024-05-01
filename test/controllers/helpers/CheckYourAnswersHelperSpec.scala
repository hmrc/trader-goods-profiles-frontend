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

      "must return empty summary list when no trader goods profile" in {
        val traderGoodsProfile: Option[TraderGoodsProfile] = None
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe Seq.empty
      }

      "must return empty summary list when blank trader goods profile" in {
        val traderGoodsProfile: Option[TraderGoodsProfile] = Some(TraderGoodsProfile())
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe Seq.empty
      }

      "must return full summary list when trader goods profile has all fields" in {
        val expected                                       = List(
          SummaryListRow(
            Key(HtmlContent("UKIMS number")),
            Value(HtmlContent(UkimsNumber("11"))),
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
            Value(HtmlContent(NirmsNumber("22")), ""),
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
            Value(HtmlContent(NiphlNumber("33")), ""),
            "",
            Some(
              Actions(
                "",
                List(ActionItem("/trader-goods-profiles/niphl-number", HtmlContent("Change"), None, "", Map()))
              )
            )
          )
        )
        val traderGoodsProfile: Option[TraderGoodsProfile] = Some(
          TraderGoodsProfile(
            Some(UkimsNumber("11")),
            Some(true),
            Some(NirmsNumber("22")),
            Some(true),
            Some(NiphlNumber("33"))
          )
        )
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(traderGoodsProfile)
        summaryList mustBe Seq.empty
      }
    }
  }
}
