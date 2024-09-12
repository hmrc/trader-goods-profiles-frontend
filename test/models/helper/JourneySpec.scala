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

package models.helper

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import utils.SessionData._

class JourneySpec extends AnyFreeSpec with Matchers {

  "CreateProfileJourney" - {
    "must have the correct pages" in {
      CreateProfileJourney.pages mustBe Seq(
        ukimsNumberPage,
        hasNirmsPage,
        hasNiphlPage,
        nirmsNumberPage,
        niphlNumberPage,
        useExistingUkimsNumberPage,
        historicProfileDataQuery
      )
    }

    "must have the correct string representation" in {
      CreateProfileJourney.toString mustBe "CreateProfile"
    }
  }

  "CreateRecordJourney" - {
    "must have the correct pages" in {
      CreateRecordJourney.pages mustBe Seq(
        traderReferencePage,
        useTraderReferencePage,
        goodsDescriptionPage,
        countryOfOriginPage,
        commodityCodePage,
        hasCorrectGoodsPage
      )
    }

    "must have the correct string representation" in {
      CreateRecordJourney.toString mustBe "CreateRecord"
    }
  }

  "UpdateRecordJourney" - {
    "must have the correct string representation" in {
      UpdateRecordJourney.toString mustBe "UpdateRecord"
    }
  }

  "CategorisationJourney" - {
    "must have the correct pages" in {
      CategorisationJourney.pages mustBe Seq(
        assessmentsPage,
        hasSupplementaryUnitPage,
        supplementaryUnitPage,
        longerCommodityCodePage,
        reassessmentPage,
        categorisationDetailsQuery,
        longerCommodityQuery,
        longerCategorisationDetailsQuery
      )
    }

    "must have the correct string representation" in {
      CategorisationJourney.toString mustBe "Categorisation"
    }
  }

  "RequestAdviceJourney" - {
    "must have the correct pages" in {
      RequestAdviceJourney.pages mustBe Seq(
        namePage,
        emailPage
      )
    }

    "must have the correct string representation" in {
      RequestAdviceJourney.toString mustBe "RequestAdvice"
    }
  }

  "WithdrawAdviceJourney" - {
    "must have the correct pages" in {
      WithdrawAdviceJourney.pages mustBe Seq(
        withDrawAdviceStartPage,
        reasonForWithdrawAdvicePage
      )
    }

    "must have the correct string representation" in {
      WithdrawAdviceJourney.toString mustBe "WithdrawAdvice"
    }
  }

  "SupplementaryUnitUpdateJourney" - {
    "must have the correct pages" in {
      SupplementaryUnitUpdateJourney.pages mustBe Seq(
        hasSupplementaryUnitUpdatePage,
        supplementaryUnitUpdatePage,
        measurementUnitQuery
      )
    }

    "must have the correct string representation" in {
      SupplementaryUnitUpdateJourney.toString mustBe "SupplementaryUnitUpdate"
    }
  }
}
