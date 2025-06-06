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
import viewmodels.checkAnswers.goodsRecord.ProductReferenceSummary

class ProductReferenceSummarySpec extends SpecBase {
  implicit private val messages: Messages = messages(applicationBuilder().build())

  "ProductReferenceSummary.row" - {
    "must return a SummaryListRow without change links when record is locked" in {
      val recordLocked = true
      val row = ProductReferenceSummary.row(recordForTestingSummaryRows.traderRef, testRecordId, NormalMode, recordLocked)

      row.actions mustBe Some(Actions("", List()))
    }

    "must return a SummaryListRow with change links when record is not locked" in {
      val recordLocked = false
      val row = ProductReferenceSummary.row(recordForTestingSummaryRows.traderRef, testRecordId, NormalMode, recordLocked)

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual controllers.goodsRecord.productReference.routes.UpdateProductReferenceController.onPageLoad(NormalMode, testRecordId).url
    }
  }
}