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

import base.TestConstants.{testRecordId, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import pages.goodsRecord._
import queries.CommodityUpdateQuery

import java.time.Instant

class UpdateGoodsRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return an UpdateGoodsRecord when all mandatory questions are answered" - {

      "and all country of origin data is present when record is categorised" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CountryOfOriginUpdatePage(testRecordId), "CN")
            .success
            .value
            .set(HasCountryOfOriginChangePage(testRecordId), true)
            .success
            .value

        val result = UpdateGoodsRecord.validateCountryOfOrigin(answers, testRecordId, isCategorised = true)

        result mustBe Right("CN")
      }

      "and all country of origin data is present when record is not categorised" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CountryOfOriginUpdatePage(testRecordId), "CN")
            .success
            .value

        val result = UpdateGoodsRecord.validateCountryOfOrigin(answers, testRecordId, isCategorised = false)

        result mustBe Right("CN")
      }

      "and all goods description data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(GoodsDescriptionUpdatePage(testRecordId), "goods description")
            .success
            .value

        val result = UpdateGoodsRecord.validateGoodsDescription(answers, testRecordId)

        result mustBe Right("goods description")

      }

      "and all product reference data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(ProductReferenceUpdatePage(testRecordId), "product reference")
            .success
            .value

        val result = UpdateGoodsRecord.validateproductReference(answers, testRecordId)

        result mustBe Right("product reference")
      }

      "and all commodity code data is present when record is categorised" in {
        val effectiveFrom = Instant.now
        val effectiveTo   = effectiveFrom.plusSeconds(99)
        val commodity     =
          Commodity(
            "1704900000",
            List(
              "Sea urchins",
              "Live, fresh or chilled",
              "Aquatic invertebrates other than crustaceans and molluscs "
            ),
            effectiveFrom,
            Some(effectiveTo)
          )

        val answers =
          UserAnswers(userAnswersId)
            .set(CommodityCodeUpdatePage(testRecordId), "170490")
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value
            .set(HasCommodityCodeChangePage(testRecordId), true)
            .success
            .value
            .set(CommodityUpdateQuery(testRecordId), commodity)
            .success
            .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = true,
          isCommCodeExpired = false
        )

        result mustBe Right(commodity.copy(commodityCode = "170490"))
      }

      "and all commodity code data is present when record is not categorised" in {
        val effectiveFrom = Instant.now
        val effectiveTo   = effectiveFrom.plusSeconds(99)
        val commodity     =
          Commodity(
            "1704900000",
            List(
              "Sea urchins",
              "Live, fresh or chilled",
              "Aquatic invertebrates other than crustaceans and molluscs "
            ),
            effectiveFrom,
            Some(effectiveTo)
          )

        val answers =
          UserAnswers(userAnswersId)
            .set(CommodityCodeUpdatePage(testRecordId), "170490")
            .success
            .value
            .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
            .success
            .value
            .set(CommodityUpdateQuery(testRecordId), commodity)
            .success
            .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = false,
          isCommCodeExpired = false
        )

        result mustBe Right(commodity.copy(commodityCode = "170490"))
      }
    }

    "must return errors" - {

      "when commodity code prefix does not match commodity in CommodityUpdateQuery" in {
        val effectiveFrom = Instant.now
        val effectiveTo   = effectiveFrom.plusSeconds(99)
        val commodity     = Commodity(
          "9999999999",
          List("Invalid commodity"),
          effectiveFrom,
          Some(effectiveTo)
        )

        val answers = UserAnswers(userAnswersId)
          .set(CommodityCodeUpdatePage(testRecordId), "170490")
          .success
          .value
          .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
          .success
          .value
          .set(CommodityUpdateQuery(testRecordId), commodity)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = false,
          isCommCodeExpired = false
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only MismatchedPage(CommodityCodeUpdatePage(testRecordId))
        }
      }

      "when country of origin is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCountryOfOriginChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.validateCountryOfOrigin(answers, testRecordId, isCategorised = true)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(CountryOfOriginUpdatePage(testRecordId))
        }
      }

      "when country of origin warning page is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCountryOfOriginChangePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.validateCountryOfOrigin(answers, testRecordId, isCategorised = true)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCountryOfOriginChangePage(testRecordId))
        }
      }

      "when goods description is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.validateGoodsDescription(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(GoodsDescriptionUpdatePage(testRecordId))
        }
      }

      "when product reference is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.validateproductReference(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(ProductReferenceUpdatePage(testRecordId))
        }
      }

      "when commodity code is required and is missing when accessed from update commodity code page" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = true,
          isCommCodeExpired = true
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(CommodityCodeUpdatePage(testRecordId))
        }
      }

      "when commodity code is required and is missing when accessed from expired commodity code page" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = false,
          isCommCodeExpired = true
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(CommodityCodeUpdatePage(testRecordId))
        }
      }

      "when has correct goods is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), true)
          .success
          .value
          .set(CommodityCodeUpdatePage(testRecordId), "test")
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = true,
          isCommCodeExpired = false
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId))
        }
      }

      "when has correct goods is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), true)
          .success
          .value
          .set(CommodityCodeUpdatePage(testRecordId), "test")
          .success
          .value
          .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = true,
          isCommCodeExpired = false
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId))
        }
      }

      "when commodity code warning page is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(
          answers,
          testRecordId,
          isCategorised = true,
          isCommCodeExpired = false
        )

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCommodityCodeChangePage(testRecordId))
        }
      }
    }
  }
}
