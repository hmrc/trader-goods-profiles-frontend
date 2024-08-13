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
import controllers.routes
import models.NormalMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import utils.Constants.adviceProvided

class GoodsDescriptionSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())

  ".rowUpdate" - {

    "must return a SummaryListRow without change link when record is locked" in {

      val row =
        GoodsDescriptionSummary.rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordLocked = true)

      row.actions mustBe Some(Actions("", List()))
    }

    "must return a SummaryListRow with change link when record is not locked" - {

      "and advice has not been provided" in {

        val row =
          GoodsDescriptionSummary.rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordLocked = false)

        row.actions mustBe defined
        row.actions.value.items.head.href mustEqual routes.GoodsDescriptionController
          .onPageLoadUpdate(NormalMode, testRecordId)
          .url
      }

      "and advice has been provided" in {

        val recordWithAdviceProvided = recordForTestingSummaryRows.copy(adviceStatus = adviceProvided)

        val row =
          GoodsDescriptionSummary.rowUpdate(recordWithAdviceProvided, testRecordId, NormalMode, recordLocked = false)

        row.actions mustBe defined
        row.actions.value.items.head.href mustEqual routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, testRecordId)
          .url
      }
    }
  }
}
