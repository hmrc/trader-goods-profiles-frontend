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

import base.TestConstants.{testEori, testRecordId, userAnswersId}
import org.scalatest.Inside.inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import queries.CommodityUpdateQuery

import java.time.Instant

class UpdateGoodsRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return an UpdateGoodsRecord when all mandatory questions are answered" - {

      "and all country of origin data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CountryOfOriginUpdatePage(testRecordId), "CN")
            .success
            .value
            .set(HasCountryOfOriginChangePage(testRecordId), true)
            .success
            .value

        val result = UpdateGoodsRecord.buildCountryOfOrigin(answers, testEori, testRecordId)

        result mustEqual Right(
          UpdateGoodsRecord(testEori, testRecordId, Some("CN"), category = Some(1))
        )
      }

      "and all goods description data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(GoodsDescriptionUpdatePage(testRecordId), "goods description")
            .success
            .value
            .set(HasGoodsDescriptionChangePage(testRecordId), true)
            .success
            .value

        val result = UpdateGoodsRecord.validateGoodsDescription(answers, testRecordId)

        result mustEqual Right("goods description")

      }

      "and all trader reference data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferenceUpdatePage(testRecordId), "trader reference")
            .success
            .value

        val result = UpdateGoodsRecord.validateTraderReference(answers, testRecordId)

        result mustEqual Right("trader reference")
      }

      "and all commodity code data is present" in {
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

        val result = UpdateGoodsRecord.validateCommodityCode(answers, testRecordId)

        result mustBe Right(commodity.copy(commodityCode = "170490"))
      }
    }

    "must return errors" - {

      "when country of origin is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCountryOfOriginChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.buildCountryOfOrigin(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(CountryOfOriginUpdatePage(testRecordId))
        }
      }

      "when country of origin warning page is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCountryOfOriginChangePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.buildCountryOfOrigin(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCountryOfOriginChangePage(testRecordId))
        }
      }

      "when goods description is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasGoodsDescriptionChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.validateGoodsDescription(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(GoodsDescriptionUpdatePage(testRecordId))
        }
      }

      "when goods description warning page is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasGoodsDescriptionChangePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.validateGoodsDescription(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasGoodsDescriptionChangePage(testRecordId))
        }
      }

      "when trader reference is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.validateTraderReference(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(TraderReferenceUpdatePage(testRecordId))
        }
      }

      "when commodity code is required and is missing" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), true)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(answers, testRecordId)

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

        val result = UpdateGoodsRecord.validateCommodityCode(answers, testRecordId)

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

        val result = UpdateGoodsRecord.validateCommodityCode(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId))
        }
      }

      "when commodity code warning page is false" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasCommodityCodeChangePage(testRecordId), false)
          .success
          .value

        val result = UpdateGoodsRecord.validateCommodityCode(answers, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only UnexpectedPage(HasCommodityCodeChangePage(testRecordId))
        }
      }
    }
  }
}
