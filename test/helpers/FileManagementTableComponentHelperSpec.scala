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

package helpers

import base.SpecBase
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent

class FileManagementTableComponentHelperSpec extends SpecBase {

  private implicit class HtmlContentOps(htmlContent: HtmlContent) {
    def toTestableString: String = htmlContent.asHtml.toString().replaceAll("\\s+", " ").trim
  }

  "FileManagementTableComponentHelper" - {
    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .build()

    val fileManagementTableComponentHelper: FileManagementTableComponentHelper =
      application.injector.instanceOf[FileManagementTableComponentHelper]

    "createTag" - {
      "must return correct html" in {
        val tag = fileManagementTableComponentHelper.createTag("tagText").toTestableString
        tag mustEqual """<strong class="govuk-tag"> tagText </strong>"""
      }
    }

    "createLink" - {
      "must return correct html" in {
        val link =
          fileManagementTableComponentHelper.createLink("text", "hiddenText", "url", "fileSize").toTestableString
        link mustEqual """<a class="govuk-link govuk-!-font-weight-bold" href="url" download="url" >text <span class="new-line">(fileSizeKB)</span><span class="govuk-visually-hidden"> hiddenText</span></a>"""
      }
    }
  }
}
