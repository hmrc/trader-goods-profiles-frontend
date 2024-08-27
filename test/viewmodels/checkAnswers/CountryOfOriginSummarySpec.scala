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
import models.{Country, NormalMode}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions

class CountryOfOriginSummarySpec extends SpecBase {

  implicit private val messages: Messages = messages(applicationBuilder().build())
  private val countries                   = Seq(Country("CN", "China"), Country("US", "United States"), Country("UK", "United Kingdom"))
  private val testCountryName             = "United Kingdom"

  "must return a SummaryListRow without change links when record is locked" in {

    val row = CountryOfOriginSummary.rowUpdate(
      recordForTestingSummaryRows,
      testRecordId,
      NormalMode,
      recordLocked = true,
      countries
    )

    row.actions mustBe Some(Actions("", List()))
  }

  "must return a SummaryListRow with change links when record is not locked" - {

    "and category is set" in {

      val row = CountryOfOriginSummary.rowUpdate(
        recordForTestingSummaryRows,
        testRecordId,
        NormalMode,
        recordLocked = false,
        countries
      )

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual routes.HasCountryOfOriginChangeController
        .onPageLoad(NormalMode, testRecordId)
        .url
    }

    "and category is not set" in {

      val recordNoCategory = recordForTestingSummaryRows.copy(category = None)

      val row = CountryOfOriginSummary.rowUpdate(
        recordNoCategory,
        testRecordId,
        NormalMode,
        recordLocked = false,
        countries
      )

      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual routes.CountryOfOriginController
        .onPageLoadUpdate(NormalMode, testRecordId)
        .url
    }

    "must display country name instead of country code" in {

      val recordNoCategory = recordForTestingSummaryRows.copy(category = None)

      val row = CountryOfOriginSummary.rowUpdate(
        recordNoCategory,
        testRecordId,
        NormalMode,
        recordLocked = false,
        countries
      )
      row.value.content.asHtml.toString() must include(testCountryName)
      row.actions mustBe defined
      row.actions.value.items.head.href mustEqual routes.CountryOfOriginController
        .onPageLoadUpdate(NormalMode, testRecordId)
        .url
    }
  }

}
