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

    "category1ToAnswer" - {

      "must include assessment with no answers if trader is NOT Niphl authorised" in {
        val categoryAssessments = Seq(
          CategoryAssessment("1", 1, Seq.empty, "description", None)
        )
        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe categoryAssessments
      }

      "must filter out assessment with no answers if trader IS Niphl authorised" in {
        val categoryAssessments = Seq(
          CategoryAssessment("1", 1, Seq.empty, "description", None)
        )
        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIPHL-only assessment if trader IS authorised" in {
        val exemptions          = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))
        val categoryAssessments = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )
        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIPHL-only assessment if trader NOT authorised" in {
        val exemptions          = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))
        val categoryAssessments = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )
        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe Seq.empty
      }

      "must NOT filter out assessment with NIPHL and other exemptions if trader NOT authorised" in {
        val exemptions          = Seq(
          OtherExemption(NiphlCode, NiphlCode, "description"),
          Certificate("Y123", "Y123", "description")
        )
        val categoryAssessments = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )
        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe categoryAssessments
      }
    }

    "category2ToAnswer" - {

      "must include assessment with no answers if trader is NOT Nirms authorised" in {
        val categoryAssessments = Seq(
          CategoryAssessment("1", 2, Seq.empty, "description", None)
        )
        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe categoryAssessments
      }

      "must filter out assessment with no answers if trader IS Nirms authorised" in {
        val categoryAssessments = Seq(
          CategoryAssessment("1", 2, Seq.empty, "description", None)
        )
        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIRMS-only assessment if trader IS authorised" in {
        val exemptions          = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))
        val categoryAssessments = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )
        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIRMS-only assessment if trader NOT authorised" in {
        val exemptions          = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))
        val categoryAssessments = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )
        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe Seq.empty
      }

      "must NOT filter out assessment with NIRMS and other exemptions if trader NOT authorised" in {
        val exemptions          = Seq(
          OtherExemption(NiphlCode, NiphlCode, "description"),
          Certificate("Y123", "Y123", "description")
        )
        val categoryAssessments = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )
        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe categoryAssessments
      }
    }

    "empty inputs" - {
      "category1ToAnswer returns empty for empty input" in {
        Seq.empty[CategoryAssessment].category1ToAnswer(isTraderNiphlAuthorised = true) mustBe empty
      }
      "category2ToAnswer returns empty for empty input" in {
        Seq.empty[CategoryAssessment].category2ToAnswer(isTraderNirmsAuthorised = true) mustBe empty
      }
    }
  }

  "CategoryAssessment.needsAnswerEvenIfNoExemptions" - {

    val niphlExemption = OtherExemption(NiphlCode, NiphlCode, "desc")
    val nirmsExemption = OtherExemption(NirmsCode, NirmsCode, "desc")
    val otherExemption = Certificate("Y123", "Y123", "desc")

    "returns true if exemptions exist regardless of trader authorisation" in {
      val assessment = CategoryAssessment("1", 1, Seq(otherExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = true) mustBe true
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "returns true for Category1 with no exemptions if trader NOT Niphl authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "returns false for Category1 with no exemptions if trader IS Niphl authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) mustBe false
    }

    "returns true for Category2 with no exemptions if trader NOT Nirms authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "returns false for Category2 with no exemptions if trader IS Nirms authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) mustBe false
    }

    "returns true for Category1 with Niphl exemption if trader NOT authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq(niphlExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "returns false for Category1 with Niphl exemption if trader authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq(niphlExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) mustBe false
    }

    "returns true for Category2 with Nirms exemption if trader NOT authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq(nirmsExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "returns false for Category2 with Nirms exemption if trader authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq(nirmsExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) mustBe false
    }

    "returns false for non-category 1 or 2 with no exemptions" in {
      val assessment = CategoryAssessment("id", 3, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe false
    }
  }
}
