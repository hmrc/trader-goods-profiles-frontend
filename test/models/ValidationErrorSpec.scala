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

import base.SpecBase
import play.api.libs.json.*
import queries.Gettable

class ValidationErrorSpec extends SpecBase {

  private val query: Gettable[String] = new Gettable[String] {
    val path: JsPath = JsPath \ "page"
  }

  "ValidationErrorSpec" - {
    "PageMissing should correctly create the message" in {
      val error = PageMissing(query)
      error.message mustBe "Page missing: /page"
    }

    "UnexpectedPage should correctly create the message" in {
      val error = UnexpectedPage(query)
      error.message mustBe "Unexpected page: /page"
    }

    "MismatchedPage should correctly create the message" in {
      val error = MismatchedPage(query)
      error.message mustBe "Mismatched page: /page"
    }

    "IncorrectlyAnsweredPage should correctly create the message" in {
      val error = IncorrectlyAnsweredPage(query)
      error.message mustBe "Incorrectly answered page: /page"
    }

    "RecordIdMissing should correctly create the message" in {
      val error = RecordIdMissing(query)
      error.message mustBe "Record ID Missing: /page"
    }

    "UnexpectedNoExemption should correctly create the message" in {
      val error = UnexpectedNoExemption(query)
      error.message mustBe "Assessment page has answer No Exemption but subsequent pages have been answered: /page"

    }

    "MissingAssessmentAnswers should correctly create the message" in {
      val error = MissingAssessmentAnswers(query)
      error.message mustBe "At least one required assessment page is missing an answer: /page"
    }

    "NoCategorisationDetailsForRecordId should correctly create the message with record ID" in {
      val error = NoCategorisationDetailsForRecordId(query, "record123")
      error.message mustBe "No categorisation details for record id record123"
    }
  }

}
