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
import base.TestConstants.testRecordId
import controllers.goodsRecord.countryOfOrigin.routes
import models.AdviceStatus.{AdviceReceived, AdviceRequestWithdrawn, NotRequested}
import models.ReviewReason.{Country, *}
import play.api.i18n.*
import play.api.libs.json.*
import play.api.mvc.Call
import play.api.test.Helpers.*
implicit val messagesApi: MessagesApi = new DefaultMessagesApi()
implicit val lang: Lang               = Lang("en")

class ReviewReasonSpec extends SpecBase {

  "ReviewReason" - {
    implicit val messages: Messages = {
      val messagesApi = stubMessagesApi(
        Map(
          "en" -> Map(
            "singleRecord.reviewReason.tagText"                         -> "No longer valid",
            "singleRecord.countryReviewReason.categorised.paragraph"    -> "The country of origin is {0} which means the category is also no longer valid.",
            "singleRecord.countryReviewReason.notCategorised.paragraph" -> "The country of origin is: {0}",
            "singleRecord.reviewReason.paragraph"                       -> "You need to {0} and then categorise this record to see if you can use it on an Internal Market Movement Information (IMMI).",
            "singleRecord.countryReviewReason.linkText"                 -> "change the country of origin"
          )
        )
      )
      MessagesImpl(Lang("en"), messagesApi)
    }

    "must deserialize from json" - {

      "when Commodity" in {
        Json.fromJson[ReviewReason](JsString("commodity")) mustBe JsSuccess(Commodity)
      }

      "when Country" in {
        Json.fromJson[ReviewReason](JsString("country")) mustBe JsSuccess(Country)
      }

      "when Inadequate" in {
        Json.fromJson[ReviewReason](JsString("inadequate")) mustBe JsSuccess(Inadequate)
      }

      "when Unclear" in {
        Json.fromJson[ReviewReason](JsString("Unclear")) mustBe JsSuccess(Unclear)
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

      "when Country" in {
        Json.toJson(Country: ReviewReason) mustBe JsString("country")
      }

      "when Inadequate" in {
        Json.toJson(Inadequate: ReviewReason) mustBe JsString("inadequate")
      }

      "when Unclear" in {
        Json.toJson(Unclear: ReviewReason) mustBe JsString("Unclear")
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
          Commodity.messageKey mustBe "singleRecord.reviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId", NotRequested)
            .value
            .url mustBe
            controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
              .onPageLoad(NormalMode, "recordId")
              .url
          Commodity.setAdditionalContent(isCategorised = true, AdviceReceived) mustBe
            Some(List("singleRecord.commodityReviewReason.categorised.adviceReceived"))

        }

        "is categorised, and advice status is NotRequested" in {
          Commodity.messageKey mustBe "singleRecord.reviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId", NotRequested)
            .value
            .url mustBe
            controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
              .onPageLoad(NormalMode, "recordId")
              .url
          Commodity.setAdditionalContent(isCategorised = true, adviceStatus = NotRequested) mustBe
            Some(List("singleRecord.commodityReviewReason.categorised"))

        }

        "is not categorised, and advice status is AdviceReceived" in {
          Commodity.messageKey mustBe "singleRecord.reviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId", NotRequested)
            .value
            .url mustBe
            controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
              .onPageLoad(NormalMode, "recordId")
              .url
          Commodity.setAdditionalContent(isCategorised = false, AdviceReceived) mustBe
            Some(List("singleRecord.commodityReviewReason.adviceReceived"))
        }

        "is not categorised, and advice status is not AdviceReceived" in {
          Commodity.messageKey mustBe "singleRecord.reviewReason"
          Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
          Commodity
            .url("recordId", NotRequested)
            .value
            .url mustBe
            controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
              .onPageLoad(NormalMode, "recordId")
              .url
          Commodity.setAdditionalContent(isCategorised = false, AdviceRequestWithdrawn) mustBe
            Some(List("singleRecord.commodityReviewReason.notCategorised.noAdvice"))
        }
      }

      "when Inadequate" in {
        Inadequate.messageKey mustBe "singleRecord.inadequateReviewReason"
        Inadequate.linkKey mustBe Some("singleRecord.inadequateReviewReason.linkText")
        Inadequate
          .url("recordId", NotRequested)
          .value
          .url mustBe
          controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
            .onPageLoad(NormalMode, "recordId")
            .url
        Inadequate.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Unclear" in {
        Unclear.messageKey mustBe "singleRecord.unclearReviewReason"
        Unclear.linkKey mustBe Some("singleRecord.unclearReviewReason.linkText")
        Unclear
          .url("recordId", NotRequested)
          .value
          .url mustBe
          controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
            .onPageLoad(NormalMode, "recordId")
            .url
        Unclear.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Measure" in {
        Measure.messageKey mustBe "singleRecord.measureReviewReason"
        Measure.linkKey mustBe Some("singleRecord.measureReviewReason.linkText")
        Measure.url("recordId", NotRequested).value.url mustBe controllers.routes.ValidateCommodityCodeController
          .changeCategory("recordId")
          .url
        Measure.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "when Mismatch" in {
        Mismatch.messageKey mustBe "singleRecord.mismatchReviewReason"
        Mismatch.linkKey mustBe None
        Mismatch.url("recordId", NotRequested) mustBe None
        Mismatch.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
      }

      "Commodity.setAdditionalContent fallback" in {
        Commodity.setAdditionalContent(isCategorised = true, AdviceRequestWithdrawn) mustBe None
      }

      "Inadequate.url with AdviceReceived" in {
        val url = Inadequate.url("recordId", AdviceReceived).value.url
        url mustBe controllers.goodsRecord.goodsDescription.routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, "recordId")
          .url
      }

      "Unclear.url with AdviceReceived" in {
        val url = Unclear.url("recordId", AdviceReceived).value.url
        url mustBe controllers.goodsRecord.goodsDescription.routes.HasGoodsDescriptionChangeController
          .onPageLoad(NormalMode, "recordId")
          .url
      }

      "is categorised, and advice status is NotRequested" in {
        Commodity.messageKey mustBe "singleRecord.reviewReason"
        Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
        Commodity
          .url("recordId", NotRequested)
          .value
          .url mustBe
          controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
            .onPageLoad(NormalMode, "recordId")
            .url
        Commodity.setAdditionalContent(isCategorised = true, NotRequested) mustBe
          Some(List("singleRecord.commodityReviewReason.categorised"))
      }

      "is not categorised, and advice status is AdviceReceived" in {
        Commodity.messageKey mustBe "singleRecord.reviewReason"
        Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
        Commodity
          .url("recordId", NotRequested)
          .value
          .url mustBe
          controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
            .onPageLoad(NormalMode, "recordId")
            .url
        Commodity.setAdditionalContent(isCategorised = false, AdviceReceived) mustBe
          Some(List("singleRecord.commodityReviewReason.adviceReceived"))
      }
      "is not categorised, and advice status is not AdviceReceived" in {
        Commodity.messageKey mustBe "singleRecord.reviewReason"
        Commodity.linkKey mustBe Some("singleRecord.commodityReviewReason.linkText")
        Commodity
          .url("recordId", NotRequested)
          .value
          .url mustBe
          controllers.goodsRecord.commodityCode.routes.HasCommodityCodeChangedController
            .onPageLoad(NormalMode, "recordId")
            .url
        Commodity.setAdditionalContent(isCategorised = false, AdviceRequestWithdrawn) mustBe
          Some(List("singleRecord.commodityReviewReason.notCategorised.noAdvice"))
      }
    }
    "when Inadequate" in {
      Inadequate.messageKey mustBe "singleRecord.inadequateReviewReason"
      Inadequate.linkKey mustBe Some("singleRecord.inadequateReviewReason.linkText")
      Inadequate
        .url("recordId", NotRequested)
        .value
        .url mustBe
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
          .onPageLoad(NormalMode, "recordId")
          .url
      Inadequate.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
    }
    "when Unclear" in {
      Unclear.messageKey mustBe "singleRecord.unclearReviewReason"
      Unclear.linkKey mustBe Some("singleRecord.unclearReviewReason.linkText")
      Unclear
        .url("recordId", NotRequested)
        .value
        .url mustBe
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController
          .onPageLoad(NormalMode, "recordId")
          .url
      Unclear.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
    }
    "when Measure" in {
      Measure.messageKey mustBe "singleRecord.measureReviewReason"
      Measure.linkKey mustBe Some("singleRecord.measureReviewReason.linkText")
      Measure.url("recordId", NotRequested).value.url mustBe controllers.routes.ValidateCommodityCodeController
        .changeCategory("recordId")
        .url
      Measure.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
    }
    "when Mismatch" in {
      Mismatch.messageKey mustBe "singleRecord.mismatchReviewReason"
      Mismatch.linkKey mustBe None
      Mismatch.url("recordId", NotRequested) mustBe None
      Mismatch.setAdditionalContent(isCategorised = false, NotRequested) mustBe None
    }

    "Country ReviewReason" - {

      "have correct messageKey and linkKey" in {
        Country.messageKey mustBe "singleRecord.reviewReason"
        Country.linkKey mustBe Some("singleRecord.countryReviewReason.linkText")
      }

      "return correct url" in {
        val expectedUrl: Call =
          routes.HasCountryOfOriginChangeController.onPageLoad(NormalMode, testRecordId)
        Country.url(testRecordId, NotRequested) mustBe Some(expectedUrl)
        Country.url(testRecordId, AdviceReceived) mustBe Some(expectedUrl)
      }

      "setAdditionalContent when categorised" in {
        val expectedTagHtml = """<span class="govuk-tag govuk-tag--grey">No longer valid</span>"""
        val result          = Country.setAdditionalContent(isCategorised = true, AdviceReceived)
        result mustBe Some(
          List(
            "The country of origin is {0} which means the category is also no longer valid."
              .replace("{0}", expectedTagHtml)
          )
        )
      }

      "setAdditionalContent when not categorised" in {
        val expectedTagHtml = """<span class="govuk-tag govuk-tag--grey">No longer valid</span>"""
        val expectedList    = List(
          s"The country of origin is: $expectedTagHtml",
          "You need to change the country of origin and then categorise this record to see if you can use it on an Internal Market Movement Information (IMMI)."
        )
        val result          = Country.setAdditionalContent(isCategorised = false, NotRequested)
        result mustBe Some(expectedList)
      }

    }

  }
}
