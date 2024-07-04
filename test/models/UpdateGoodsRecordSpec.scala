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

class UpdateGoodsRecordSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return an UpdateGoodsRecord when all mandatory questions are answered" - {

      "and all country of origin data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(CountryOfOriginPage(testRecordId), "1")
            .success
            .value

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, CountryOfOriginPageUpdate)

        result mustEqual Right(
          UpdateGoodsRecord(testEori, testRecordId, Some("1"))
        )
      }

      "and all goods description data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(GoodsDescriptionPage(testRecordId), "1")
            .success
            .value

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, GoodsDescriptionPageUpdate)

        result mustEqual Right(
          UpdateGoodsRecord(testEori, testRecordId, goodsDescription = Some("1"))
        )
      }

      "and all trader reference data is present" in {

        val answers =
          UserAnswers(userAnswersId)
            .set(TraderReferencePage(testRecordId), "1")
            .success
            .value

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, TraderReferencePageUpdate)

        result mustEqual Right(
          UpdateGoodsRecord(testEori, testRecordId, traderReference = Some("1"))
        )
      }
    }

    "must return errors" - {

      "when country of origin is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, CountryOfOriginPageUpdate)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(CountryOfOriginPage(testRecordId))
        }
      }

      "when goods description is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, GoodsDescriptionPageUpdate)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(GoodsDescriptionPage(testRecordId))
        }
      }

      "when trader reference is required and is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = UpdateGoodsRecord.build(answers, testEori, testRecordId, TraderReferencePageUpdate)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain only PageMissing(TraderReferencePage(testRecordId))
        }
      }
    }
  }
}
