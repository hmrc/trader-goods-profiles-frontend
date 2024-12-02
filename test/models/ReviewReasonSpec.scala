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

package models

import base.SpecBase
import models.ReviewReason._
import play.api.libs.json._

class ReviewReasonSpec extends SpecBase {

  "ReviewReason" - {

    "must deserialize from json" - {

      "when Commodity" in {
        Json.fromJson[ReviewReason](JsString("commodity")) mustBe JsSuccess(Commodity)
      }

      "when Unclear" in {
        Json.fromJson[ReviewReason](JsString("unclear")) mustBe JsSuccess(Unclear)
      }

      "when Measure" in {
        Json.fromJson[ReviewReason](JsString("measure")) mustBe JsSuccess(Measure)
      }

      "when Mismatch" in {
        Json.fromJson[ReviewReason](JsString("mismatch")) mustBe JsSuccess(Mismatch)
      }
    }

    "must serialize to json" - {

      "when Commodity" in {
        Json.toJson(Commodity: ReviewReason) mustBe JsString("commodity")
      }

      "when Unclear" in {
        Json.toJson(Unclear: ReviewReason) mustBe JsString("unclear")
      }

      "when Measure" in {
        Json.toJson(Measure: ReviewReason) mustBe JsString("measure")
      }

      "when Mismatch" in {
        Json.toJson(Mismatch: ReviewReason) mustBe JsString("mismatch")
      }
    }

    "must have correct information" - {

      "when Commodity" in {
        Commodity.messageKey mustBe "singleRecord.commodityReviewReason"
        Commodity.linkKey mustBe None
        Commodity.url("recordId").value.url mustBe controllers.problem.routes.JourneyRecoveryController
          .onPageLoad()
          .url
      }

      "when Unclear" in {
        Unclear.messageKey mustBe "singleRecord.unclearReviewReason"
        Unclear.linkKey mustBe Some("singleRecord.unclearReviewReason.linkText")
        Unclear.url("recordId").value.url mustBe controllers.goodsRecord.routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, "recordId")
          .url
      }

      "when Measure" in {
        Measure.messageKey mustBe "singleRecord.measureReviewReason"
        Measure.linkKey mustBe Some("singleRecord.measureReviewReason.linkText")
        Measure.url("recordId").value.url mustBe controllers.categorisation.routes.CategorisationPreparationController
          .startCategorisation("recordId")
          .url
      }

      "when Mismatch" in {
        Mismatch.messageKey mustBe "singleRecord.mismatchReviewReason"
        Mismatch.linkKey mustBe None
        Mismatch.url("recordId") mustBe None
      }
    }
  }

}
