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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions

class CategorySummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  "must return a SummaryListRow without change links when record is locked" in {

    val row =
      CategorySummary.row(recordForTestingSummaryRows, recordLocked = true)

    row.actions mustBe Some(Actions("", List()))
  }

  "must return a SummaryListRow with change links(CategoryGuidanceController) when record is not locked and category is defined" in {

    val row =
      CategorySummary.row(recordForTestingSummaryRows, recordLocked = false)

    row.actions mustBe defined
    row.actions.value.items.head.href mustEqual routes.CategoryGuidanceController
      .onPageLoad(recordForTestingSummaryRows.recordId)
      .url

  }

  "must return a SummaryListRow with change links(ExpiredCommodityCodeController) when record is not locked and commodity code expired on the same day" in {

    val row =
      CategorySummary.row(recordWithExpiredCommodityCode, recordLocked = false)

    row.actions mustBe defined
    row.actions.value.items.head.href mustEqual routes.ExpiredCommodityCodeController
      .onPageLoad(recordWithExpiredCommodityCode.recordId)
      .url

  }

}
