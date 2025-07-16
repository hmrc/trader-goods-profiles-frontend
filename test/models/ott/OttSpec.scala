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
          CategoryAssessment("1", 1, Seq.empty, "description", None)
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe Seq.empty
      }

      "must filter out NIPHL assessment if trader is authorised" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIPHL assessment if trader is not authorised and only NIPHL exemption available" in {

        val exemptions = Seq(OtherExemption(NiphlCode, NiphlCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe Seq.empty
      }

      "must not filter out NIPHL assessment if trader is not authorised and there is other available exemptions" in {

        val exemptions =
          Seq(OtherExemption(NiphlCode, NiphlCode, "description"), Certificate("Y123", "Y123", "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 1, exemptions, "description", None)
        )

        categoryAssessments.category1ToAnswer(isTraderNiphlAuthorised = false) mustBe categoryAssessments
      }
    }

    "category2answer" - {
      "must filter out assessment with no answers" in {
        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, Seq.empty, "description", None)
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe Seq.empty
      }

      "must filter out NIRMS assessment if trader is authorised" in {

        val exemptions = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = true) mustBe Seq.empty
      }

      "must filter out NIRMS assessment if trader is not authorised and only NIRMS exemption available" in {

        val exemptions = Seq(OtherExemption(NirmsCode, NirmsCode, "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe Seq.empty
      }

      "must not filter out NIRMS assessment if trader is not authorised and there is other available exemptions" in {

        val exemptions =
          Seq(OtherExemption(NiphlCode, NiphlCode, "description"), Certificate("Y123", "Y123", "description"))

        val categoryAssessments: Seq[CategoryAssessment] = Seq(
          CategoryAssessment("1", 2, exemptions, "description", None)
        )

        categoryAssessments.category2ToAnswer(isTraderNirmsAuthorised = false) mustBe categoryAssessments
      }
    }
  }
  "CategoryAssessment.needsAnswerEvenIfNoExemptions" - {

    val niphlExemption = OtherExemption(NiphlCode, NiphlCode, "desc")
    val nirmsExemption = OtherExemption(NirmsCode, NirmsCode, "desc")
    val otherExemption = Certificate("Y123", "Y123", "desc")

    "should return true if exemptions exist" in {
      val assessment = CategoryAssessment("1", 1, Seq(otherExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = true) mustBe true
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "should return true for Category1 with no exemptions and trader NOT Niphl authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "should return false for Category1 with no exemptions and trader IS Niphl authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) mustBe false
    }

    "should return true for Category2 with no exemptions and trader NOT Nirms authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "should return false for Category2 with no exemptions and trader IS Nirms authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) mustBe false
    }

    "should return true for Category1 with Niphl exemption and trader NOT authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq(niphlExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "should return false for Category1 with Niphl exemption and trader authorised" in {
      val assessment = CategoryAssessment("1", 1, Seq(niphlExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) mustBe false
    }

    "should return true for Category2 with Nirms exemption and trader NOT authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq(nirmsExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe true
    }

    "should return false for Category2 with Nirms exemption and trader authorised" in {
      val assessment = CategoryAssessment("2", 2, Seq(nirmsExemption), "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) mustBe false
    }

    "must return empty on empty input for category1ToAnswer" in {
      val emptySeq = Seq.empty[CategoryAssessment]
      emptySeq.category1ToAnswer(isTraderNiphlAuthorised = true) mustBe empty
    }

    "must return empty on empty input for category2ToAnswer" in {
      val emptySeq = Seq.empty[CategoryAssessment]
      emptySeq.category2ToAnswer(isTraderNirmsAuthorised = true) mustBe empty
    }

    "needsAnswerEvenIfNoExemptions returns false for non-category 1 or 2 with no exemptions" in {
      val assessment = CategoryAssessment("id", 3, Seq.empty, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) mustBe false
    }

    "needsAnswerEvenIfNoExemptions returns true if exemptions exist regardless of trader authorization" in {
      val exemptions = Seq(OtherExemption("X", "X", "desc"))
      val assessment = CategoryAssessment("id", 1, exemptions, "desc", None)
      assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = true) mustBe true
    }

  }

}
