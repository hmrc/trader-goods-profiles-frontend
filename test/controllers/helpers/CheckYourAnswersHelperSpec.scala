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
import models.{MaintainProfileAnswers, NiphlNumber, NirmsNumber, UkimsNumber}
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.govuk.SummaryListFluency

class CheckYourAnswersHelperSpec extends SpecBase with SummaryListFluency {

  "CheckYourAnswersHelper" - {

    val checkYourAnswersHelper = new CheckYourAnswersHelper()

    "createSummaryList" - {

      "must return empty summary list when blank trader goods profile" in {
        val maintainProfileAnswers: MaintainProfileAnswers = MaintainProfileAnswers()
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(maintainProfileAnswers)(messages)
        summaryList mustBe Seq.empty
      }

      "must return full summary list when trader goods profile has all fields" in {
        val maintainProfileAnswers: MaintainProfileAnswers =
          MaintainProfileAnswers(
            Some(UkimsNumber("11")),
            Some(true),
            Some(NirmsNumber("22")),
            Some(true),
            Some(NiphlNumber("33"))
          )
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(maintainProfileAnswers)(messages)
        summaryList mustBe create5RowTable()
      }

      "must return valid summary list when trader goods profile has no for NIPHL" in {
        val maintainProfileAnswers: MaintainProfileAnswers =
          MaintainProfileAnswers(
            Some(UkimsNumber("11")),
            Some(true),
            Some(NirmsNumber("22")),
            Some(false)
          )
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(maintainProfileAnswers)(messages)
        summaryList mustBe create4RowTableNoNiphl()
      }

      "must return valid summary list when trader goods profile has no for NIRMS" in {
        val maintainProfileAnswers: MaintainProfileAnswers =
          MaintainProfileAnswers(
            Some(UkimsNumber("11")),
            Some(false),
            nirmsNumber = None,
            Some(true),
            Some(NiphlNumber("33"))
          )
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(maintainProfileAnswers)(messages)
        summaryList mustBe create4RowTableNoNirms()
      }

      "must return valid summary list when trader goods profile has 2 nos" in {
        val maintainProfileAnswers: MaintainProfileAnswers =
          MaintainProfileAnswers(
            Some(UkimsNumber("11")),
            Some(false),
            nirmsNumber = None,
            Some(false),
            niphlNumber = None
          )
        val summaryList                                    = checkYourAnswersHelper.createSummaryList(maintainProfileAnswers)(messages)
        summaryList mustBe create3RowTable()
      }

    }
  }
  def create5RowTable(): Seq[SummaryListRow] = Seq(
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.ukimsNumber")),
      Value(HtmlContent("11")),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/ukims-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsRegistered"), ""),
      Value(HtmlContent("site.yes"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsNumber"), ""),
      Value(HtmlContent("22"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlRegistered"), ""),
      Value(HtmlContent("site.yes"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlNumber"), ""),
      Value(HtmlContent("33"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    )
  )

  def create4RowTableNoNirms(): Seq[SummaryListRow] = Seq(
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.ukimsNumber")),
      Value(HtmlContent("11")),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/ukims-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsRegistered"), ""),
      Value(HtmlContent("site.no"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlRegistered"), ""),
      Value(HtmlContent("site.yes"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlNumber"), ""),
      Value(HtmlContent("33"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    )
  )
  def create4RowTableNoNiphl(): Seq[SummaryListRow] = Seq(
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.ukimsNumber")),
      Value(HtmlContent("11")),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/ukims-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsRegistered"), ""),
      Value(HtmlContent("site.yes"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsNumber"), ""),
      Value(HtmlContent("22"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlRegistered"), ""),
      Value(HtmlContent("site.no"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    )
  )

  def create3RowTable(): Seq[SummaryListRow] = Seq(
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.ukimsNumber")),
      Value(HtmlContent("11")),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/ukims-number/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.nirmsRegistered"), ""),
      Value(HtmlContent("site.no"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/nirms-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    ),
    SummaryListRow(
      Key(HtmlContent("checkYourAnswers.heading.niphlRegistered"), ""),
      Value(HtmlContent("site.no"), ""),
      "",
      Some(
        Actions(
          "",
          List(
            ActionItem("/trader-goods-profiles/niphl-question/check", HtmlContent("site.change"), None, "", Map())
          )
        )
      )
    )
  )
}
