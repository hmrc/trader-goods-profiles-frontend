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
import base.TestConstants.testRecordId
import models.NormalMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions

import java.time.Instant

class HasSupplementaryUnitSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  private val recordWithSupplementaryUnitCat2 = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId, category = Some(2))

  private val recordWithSupplementaryUnitCat1 = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId, category = Some(1))

  "HasSupplementaryUnitSummary.row" - {
    "must not return a SummaryListRow when no measurement unit" in {
      val row = HasSupplementaryUnitSummary.row(goodsRecordResponse(Instant.now, Instant.now), testRecordId, recordLocked = false)

      row mustBe None
    }

    "must not return a SummaryListRow when there is a measurement unit but the category is not 2" in {
      val row = HasSupplementaryUnitSummary.row(recordWithSupplementaryUnitCat1, testRecordId, recordLocked = false)

      row mustBe None
    }

    "must return a SummaryListRow without change links when record is locked and is Category 2" in {
      val row = HasSupplementaryUnitSummary.row(recordWithSupplementaryUnitCat2, testRecordId, recordLocked = true)

      row.get.actions mustBe Some(Actions("", List()))
    }

    "must return a SummaryListRow with change links when record is not locked and is Category 2" in {
      val row = HasSupplementaryUnitSummary.row(recordWithSupplementaryUnitCat2, testRecordId, recordLocked = false)

      row.get.actions mustBe defined
      row.get.actions.value.items.head.href mustEqual controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoadUpdate(NormalMode, testRecordId).url
    }
  }
}