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
  implicit class CategoryAssessmentSeqOps(assessments: Seq[CategoryAssessment]) {

    def category1ToAnswer(isTraderNiphlAuthorised: Boolean): Seq[CategoryAssessment] =
      assessments
        .filter(_.isCategory1)
        .filter(assessment => assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = isTraderNiphlAuthorised, isTraderNirms = false))
        .filterNot(_.onlyContainsNiphlAnswer)

    def category2ToAnswer(isTraderNirmsAuthorised: Boolean): Seq[CategoryAssessment] =
      assessments
        .filter(_.isCategory2)
        .filter(assessment => assessment.needsAnswerEvenIfNoExemptions(isTraderNiphl = false, isTraderNirms = isTraderNirmsAuthorised))
        .filterNot(_.onlyContainsNirmsAnswer)

  }
}
