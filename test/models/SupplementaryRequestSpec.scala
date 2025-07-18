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
import pages.*
import pages.categorisation.HasSupplementaryUnitUpdatePage
import play.api.libs.json.{JsSuccess, Json}
import queries.MeasurementQuery

class SupplementaryRequestSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val supplementaryRequest     = SupplementaryRequest(testEori, testRecordId, Some(true), Some("1.0"), Some("kg"))
  private val supplementaryRequestJson =
    Json.obj(
      "eori"                 -> testEori,
      "recordId"             -> testRecordId,
      "hasSupplementaryUnit" -> true,
      "supplementaryUnit"    -> "1.0",
      "measurementUnit"      -> "kg"
    )

  "SupplementaryRequest" - {
    "must deserialize from json" in {
      Json.fromJson[SupplementaryRequest](supplementaryRequestJson) mustBe JsSuccess(supplementaryRequest)
    }

    "must serialize to json" in {
      Json.toJson(supplementaryRequest) mustBe supplementaryRequestJson
    }
    ".build" - {

      "must return a SupplementaryRequest when all optional data is present" in {

        val answers = UserAnswers(userAnswersId)
          .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitUpdatePage(testRecordId), "1.0")
          .success
          .value
          .set(MeasurementQuery(testRecordId), "kg")
          .success
          .value

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        result mustEqual Right(
          SupplementaryRequest(
            testEori,
            testRecordId,
            hasSupplementaryUnit = Some(true),
            supplementaryUnit = Some("1.0"),
            measurementUnit = Some("kg")
          )
        )
      }

      "must return a SupplementaryRequest when HasSupplementaryUnitUpdatePage is missing but have SupplementaryUnitUpdatePage" in {

        val answers = UserAnswers(userAnswersId)
          .set(SupplementaryUnitUpdatePage(testRecordId), "1.0")
          .success
          .value
          .set(MeasurementQuery(testRecordId), "kg")
          .success
          .value

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        result mustEqual Right(
          SupplementaryRequest(
            testEori,
            testRecordId,
            hasSupplementaryUnit = None,
            supplementaryUnit = Some("1.0"),
            measurementUnit = Some("kg")
          )
        )
      }

      "must return errors when the user said they have a SupplementaryUnit but it is missing" in {

        val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitUpdatePage(testRecordId), true).success.value

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(SupplementaryUnitUpdatePage(testRecordId)),
            PageMissing(MeasurementQuery(testRecordId))
          )
        }
      }

      "must return supplementaryrequest when user said they don't have a SupplementaryUnit and it is missing" in {

        val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitUpdatePage(testRecordId), false).success.value

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        result mustEqual Right(
          SupplementaryRequest(
            testEori,
            testRecordId,
            hasSupplementaryUnit = Some(false),
            supplementaryUnit = None,
            measurementUnit = None
          )
        )
      }

      "must return error when all data is missing" in {

        val answers = UserAnswers(userAnswersId)

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(SupplementaryUnitUpdatePage(testRecordId)),
            PageMissing(MeasurementQuery(testRecordId))
          )
        }
      }

      "when supplementaryunit is define but measurementquery is missing " in {

        val answers = UserAnswers(userAnswersId).set(SupplementaryUnitUpdatePage(testRecordId), "1.0").success.value

        val result = SupplementaryRequest.build(answers, testEori, testRecordId)

        inside(result) { case Left(errors) =>
          errors.toChain.toList must contain theSameElementsAs Seq(
            PageMissing(MeasurementQuery(testRecordId))
          )
        }
      }
    }
  }
}
