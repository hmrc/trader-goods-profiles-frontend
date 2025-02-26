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

package viewmodels

import models.AssessmentAnswer
import models.AssessmentAnswer.toSeq
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

case class AssessmentCyaValue(answers: AssessmentAnswer, codes: Seq[String], descriptions: Seq[String]) {
  def content: HtmlContent = {

    val answerCodes: Seq[String] = toSeq(answers)

    val exemptions = codes
      .zip(descriptions)

    val answerExemptions: String = answerCodes.flatMap { answerCode =>
      exemptions.collectFirst {
        case (code, description) if code == answerCode => s"<p class='govuk-body' lang='en'>$code - $description</p>"
      }
    }.mkString

    HtmlContent(
      Html(
        s"""
        $answerExemptions
      """
      )
    )
  }
}
