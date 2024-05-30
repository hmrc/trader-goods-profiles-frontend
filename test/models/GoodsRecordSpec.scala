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
import pages._

class GoodsRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a GoodsRecord when all mandatory questions are answered" - {

      "and all optional data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "654321")
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(UseTraderReferencePage, false)
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value

        val result = GoodsRecord.build(answers, testEori)

        result mustEqual Right(GoodsRecord(testEori, "123", "654321", "1", "2"))
      }

      "and all optional data is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "654321")
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(UseTraderReferencePage, true)
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value

        val result = GoodsRecord.build(answers, testEori)

        result mustEqual Right(GoodsRecord(testEori, "123", "654321", "1", "123"))
      }
    }

    "must return errors" - {

      "when all mandatory answers are missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = GoodsRecord.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(TraderReferencePage),
            PageMissing(CommodityCodePage),
            PageMissing(CountryOfOriginPage),
            PageMissing(UseTraderReferencePage)
          )
        }
      }

      "when the user said they have a Goods Description but it is missing" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "654321")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(UseTraderReferencePage, false)
            .success
            .value

        val result = GoodsRecord.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(GoodsDescriptionPage)
        }
      }

      "when the user said they don't have optional data but it is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "654321")
            .success
            .value
            .set(HasCorrectGoodsPage, true)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(UseTraderReferencePage, true)
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value

        val result = GoodsRecord.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(GoodsDescriptionPage)
          )
        }
      }

      "when HasCorrectGoodsPage is false" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage, "123")
            .success
            .value
            .set(CommodityCodePage, "654321")
            .success
            .value
            .set(HasCorrectGoodsPage, false)
            .success
            .value
            .set(CountryOfOriginPage, "1")
            .success
            .value
            .set(UseTraderReferencePage, false)
            .success
            .value
            .set(GoodsDescriptionPage, "2")
            .success
            .value

        val result = GoodsRecord.build(answers, testEori)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            UnexpectedPage(HasCorrectGoodsPage)
          )
        }
      }
    }
  }
}
