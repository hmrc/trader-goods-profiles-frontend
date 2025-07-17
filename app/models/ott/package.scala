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

package models

package object ott {
  implicit class CategoryAssessmentSeqOps(val assessments: Seq[CategoryAssessment]) extends AnyVal {

    def category1ToAnswer(isTraderNiphlAuthorised: Boolean): Seq[CategoryAssessment] = {
      assessments.filter { assessment =>
        if (isTraderNiphlAuthorised) {
          // When trader is authorised, exclude assessments with no exemptions
          // and exclude assessments with only Niphl exemptions
          assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = true, isTraderNirms = false) &&
            !assessment.onlyContainsNiphlAnswer
        } else {
          // When trader NOT authorised, include assessments with no exemptions or
          // those that have Niphl + other exemptions, but exclude those only with Niphl exemptions
          assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) &&
            !assessment.onlyContainsNiphlAnswer
        }
      }
    }

    def category2ToAnswer(isTraderNirmsAuthorised: Boolean): Seq[CategoryAssessment] = {
      assessments.filter { assessment =>
        if (isTraderNirmsAuthorised) {
          // When trader is authorised, exclude assessments with no exemptions
          // and exclude assessments with only Nirms exemptions
          assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = true) &&
            !assessment.onlyContainsNirmsAnswer
        } else {
          // When trader NOT authorised, include assessments with no exemptions or
          // those that have Nirms + other exemptions, but exclude those only with Nirms exemptions
          assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = false) &&
            !assessment.onlyContainsNirmsAnswer
        }
      }
    }
  }
}