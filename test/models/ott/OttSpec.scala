/*
 * Copyright 2025 HM Revenue & Customs
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

package models.ott

import base.TestConstants.{NiphlCode, NirmsCode}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class OttSpec extends AnyFreeSpec with Matchers {

  "CategoryAssessmentSeqOps" - {
    "category1answer" - {
      "must filter out assessment with no answers" in {
        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, Seq.empty, "description", None),
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe Seq.empty
      }

      "must filter out NIPHL assessment if trader is authorised" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None),
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIPHL assessment if trader is not authorised and only NIPHL exemption available" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None),
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe Seq.empty
      }

      "must not filter out NIPHL assessment if trader is not authorised and there is other available exemptions" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"), Certificate("Y123", "Y123", "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None),
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe categoryAssessments
      }
    }

    "category2answer" - {
      "must filter out assessment with no answers" in {
        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, Seq.empty, "description", None),
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe Seq.empty
      }

      "must filter out NIRMS assessment if trader is authorised" in {

        val exemptions = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None),
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIRMS assessment if trader is not authorised and only NIRMS exemption available" in {

        val exemptions = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None),
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe Seq.empty
      }

      "must not filter out NIRMS assessment if trader is not authorised and there is other available exemptions" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"), Certificate("Y123", "Y123", "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None),
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe categoryAssessments
      }
    }
  }
}
