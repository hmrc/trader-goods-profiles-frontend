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
import models.AdviceStatus.{AdviceReceived, AdviceRequestWithdrawn, NotRequested}
import models.ReviewReason._
import play.api.libs.json._

class ReviewReasonSpec extends SpecBase {

  "ReviewReason" - {

    "must deserialize from json" - {

      "when Commodity" in {
        Json.fromJson[ReviewReason](JsString("commodity")) mustBe JsSuccess(Commodity)
      }

      "when Inadequate" in {
        Json.fromJson[ReviewReason](JsString("inadequate")) mustBe JsSuccess(Inadequate)
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

      "when Inadequate" in {
        Json.toJson(Inadequate: ReviewReason) mustBe JsString("inadequate")
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

      "when Commodity and" - {

        "is categorised, and advice status is AdviceReceived" in {
          Commodity.messageKey mustBe "singleRecord.commodityReviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId")
            .value
            .url mustBe controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
            .onPageLoad(NormalMode, "recordId")
            .url
          Commodity.setAdditionalContent(isCategorised = true, AdviceReceived) mustBe Some(
            (
              "singleRecord.commodityReviewReason.categorised.adviceReceived",
              "singleRecord.commodityReviewReason.tagText"
            )
          )
        }

        "is categorised, and advice status is NotRequested" in {
          Commodity.messageKey mustBe "singleRecord.commodityReviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId")
            .value
            .url mustBe controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
            .onPageLoad(NormalMode, "recordId")
            .url
          Commodity.setAdditionalContent(isCategorised = true, NotRequested) mustBe Some(
            ("singleRecord.commodityReviewReason.categorised", "singleRecord.commodityReviewReason.tagText")
          )
        }

        "is not categorised, and advice status is AdviceReceived" in {
          Commodity.messageKey mustBe "singleRecord.commodityReviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId")
            .value
            .url mustBe controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
            .onPageLoad(NormalMode, "recordId")
            .url
          Commodity.setAdditionalContent(isCategorised = false, AdviceReceived) mustBe Some(
            ("singleRecord.commodityReviewReason.adviceReceived", "singleRecord.commodityReviewReason.tagText")
          )
        }

        "is not categorised, and advice status is not AdviceReceived" in {
          Commodity.messageKey mustBe "singleRecord.commodityReviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId")
            .value
            .url mustBe controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangeController
            .onPageLoad(NormalMode, "recordId")
            .url
          Commodity.setAdditionalContent(isCategorised = false, AdviceRequestWithdrawn) mustBe Some(
            ("singleRecord.commodityReviewReason.notCategorised.noAdvice", "singleRecord.commodityReviewReason.tagText")
          )
        }
      }

      "when Inadequate" in {
        Inadequate.messageKey mustBe "singleRecord.inadequateReviewReason"
        Inadequate.linkKey mustBe Some("singleRecord.inadequateReviewReason.linkText")
        Inadequate.url("recordId").value.url mustBe controllers.goodsRecord.routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, "recordId")
          .url
        Inadequate.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Unclear" in {
        Unclear.messageKey mustBe "singleRecord.unclearReviewReason"
        Unclear.linkKey mustBe Some("singleRecord.unclearReviewReason.linkText")
        Unclear.url("recordId").value.url mustBe controllers.goodsRecord.routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, "recordId")
          .url
        Unclear.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Measure" in {
        Measure.messageKey mustBe "singleRecord.measureReviewReason"
        Measure.linkKey mustBe Some("singleRecord.measureReviewReason.linkText")
        Measure.url("recordId").value.url mustBe controllers.categorisation.routes.CategorisationPreparationController
          .startCategorisation("recordId")
          .url
        Measure.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Mismatch" in {
        Mismatch.messageKey mustBe "singleRecord.mismatchReviewReason"
        Mismatch.linkKey mustBe None
        Mismatch.url("recordId") mustBe None
        Mismatch.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }
    }
  }

}
