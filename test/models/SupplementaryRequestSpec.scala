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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import queries.MeasurementQuery

class SupplementaryRequestSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".build" - {

    "must return a SupplementaryRequest when all optional data is present" in {

      val answers =
        UserAnswers(userAnswersId)
          .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitUpdatePage(testRecordId), "1.0")
          .success
          .value
          .set(MeasurementQuery, "kg")
          .success
          .value

      val result = SupplementaryRequest.build(answers, testEori, testRecordId)

      result must be(
        SupplementaryRequest(
          testEori,
          testRecordId,
          hasSupplementaryUnit = Some(true),
          supplementaryUnit = Some("1.0"),
          measurementUnit = Some("kg")
        )
      )
    }

    "must return a SupplementaryRequest when optional data is missing" in {

      val answers =
        UserAnswers(userAnswersId)
          .set(HasSupplementaryUnitUpdatePage(testRecordId), false)
          .success
          .value
          .set(MeasurementQuery, "kg")
          .success
          .value

      val result = SupplementaryRequest.build(answers, testEori, testRecordId)

      result must be(
        SupplementaryRequest(
          testEori,
          testRecordId,
          hasSupplementaryUnit = Some(false),
          supplementaryUnit = None,
          measurementUnit = Some("kg")
        )
      )
    }

    "must return a SupplementaryRequest when measurement unit is missing" in {

      val answers =
        UserAnswers(userAnswersId)
          .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
          .success
          .value
          .set(SupplementaryUnitUpdatePage(testRecordId), "1.0")
          .success
          .value

      val result = SupplementaryRequest.build(answers, testEori, testRecordId)

      result must be(
        SupplementaryRequest(
          testEori,
          testRecordId,
          hasSupplementaryUnit = Some(true),
          supplementaryUnit = Some("1.0"),
          measurementUnit = None
        )
      )
    }

    "must return a SupplementaryRequest when all data is missing" in {

      val answers = UserAnswers(userAnswersId)

      val result = SupplementaryRequest.build(answers, testEori, testRecordId)

      result must be(
        SupplementaryRequest(
          testEori,
          testRecordId,
          hasSupplementaryUnit = None,
          supplementaryUnit = None,
          measurementUnit = None
        )
      )
    }

  }
}
