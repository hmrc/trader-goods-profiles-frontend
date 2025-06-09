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

import base.TestConstants.{testEori, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.*
import pages.goodsRecord.{CommodityCodePage, CountryOfOriginPage, GoodsDescriptionPage, ProductReferencePage}
import play.api.libs.json.{JsSuccess, Json}
import queries.CommodityQuery

import java.time.Instant

class GoodsRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val testCommodity    = Commodity("1234567890", List("test"), Instant.now, None)
  private val shorterCommodity = Commodity("123456", List("test"), Instant.now, None)

  private val goodsRecord     = GoodsRecord(testEori, "123", testCommodity, "2", "1")
  private val goodsRecordJson = Json.obj(
    "eori"             -> testEori,
    "traderRef"        -> "123",
    "commodity"        -> Json.toJson(testCommodity),
    "goodsDescription" -> "2",
    "countryOfOrigin"  -> "1"
  )

  "GoodsRecord" - {
    "must deserialize from json" in {
      Json.fromJson[GoodsRecord](goodsRecordJson) mustBe JsSuccess(goodsRecord)
    }

    "must serialize to json" in {
      Json.toJson(goodsRecord) mustBe goodsRecordJson
    }

    ".build" - {

      "must return a GoodsRecord when all mandatory questions are answered" - {

        "and all optional data is present" in {

          val answers = UserAnswers(userAnswersId)
            .set(ProductReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, testCommodity.commodityCode)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CommodityQuery, testCommodity)
            .success
            .value

          val result = GoodsRecord.build(answers, testEori)

          result mustBe Right(
            GoodsRecord(testEori, "123", testCommodity, "2", "1")
          )
        }

        "and all optional data is missing" in {

          val answers = UserAnswers(userAnswersId)
            .set(ProductReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, testCommodity.commodityCode)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(GoodsDescriptionPage, "DESCRIPTION")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CommodityQuery, testCommodity)
            .success
            .value

          val result = GoodsRecord.build(answers, testEori)

          result mustBe Right(
            GoodsRecord(testEori, "123", testCommodity, "DESCRIPTION", "1")
          )
        }

        "and using short commodity code" in {

          val answers =
            UserAnswers(userAnswersId)
              .set(ProductReferencePage, "123")
              .success
              .value
              .set(CommodityCodePage, "123456")
              .success
              .value
              .set(CountryOfOriginPage, "1")
              .success
              .value
              .set(GoodsDescriptionPage, "DESCRIPTION")
              .success
              .value
              .set(HasCorrectGoodsPage, true)
              .success
              .value
              .set(CommodityQuery, shorterCommodity)
              .success
              .value

          val result = GoodsRecord.build(answers, testEori)

          result mustBe Right(
            GoodsRecord(testEori, "123", shorterCommodity, "DESCRIPTION", "1")
          )
        }
      }

      "must return errors" - {

        "when all mandatory answers are missing" in {

          val answers = UserAnswers(userAnswersId)

          val result = GoodsRecord.build(answers, testEori)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain theSameElementsAs Seq(
              PageMissing(ProductReferencePage),
              PageMissing(CommodityCodePage),
              PageMissing(CountryOfOriginPage),
              PageMissing(GoodsDescriptionPage)
            )
          }
        }

        "when the user said they have a Goods Description but it is missing" in {

          val answers = UserAnswers(userAnswersId)
            .set(ProductReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, testCommodity.commodityCode)
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(CommodityQuery, testCommodity)
            .success
            .value

          val result = GoodsRecord.build(answers, testEori)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only PageMissing(GoodsDescriptionPage)
          }
        }

        "when HasCorrectGoodsPage is false but they do have a CommodityCodePage" in {

          val answers = UserAnswers(userAnswersId)
            .set(ProductReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, testCommodity.commodityCode)
            .success
            .value
            .set(HasCorrectGoodsPage, false)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(GoodsDescriptionPage, "goods description")
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value
            .set(CommodityQuery, testCommodity)
            .success
            .value

          val result = GoodsRecord.build(answers, testEori)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only UnexpectedPage(HasCorrectGoodsPage)
          }
        }

        "when CommodityQuery code and CommodityCodePage do not match" in {

          val answers = UserAnswers(userAnswersId)
            .set(ProductReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "test")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(GoodsDescriptionPage, "goods description")
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value
            .set(CommodityQuery, testCommodity)
            .success
            .value

          val result = GoodsRecord.build(answers, testEori)

          inside(result) { case Left(errors) =>
            errors.toChain.toList must contain only MismatchedPage(CommodityCodePage)
          }
        }
      }
    }
  }
}
