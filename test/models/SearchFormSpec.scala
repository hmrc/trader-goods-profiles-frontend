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

import base.SpecBase

class SearchFormSpec extends SpecBase {

  "SearchForm" - {

    "must return None when searchTerm is None" in {
      val searchForm = SearchForm(searchTerm = None, countryOfOrigin = None)
      searchForm.trimmedSearchTerm mustBe None
    }

    "must return trimmed searchTerm when it has leading/trailing spaces" in {
      val searchForm = SearchForm(searchTerm = Some("   search term   "), countryOfOrigin = None)
      searchForm.trimmedSearchTerm mustBe Some("search term")
    }

    "must return None when searchTerm is empty string" in {
      val searchForm = SearchForm(searchTerm = Some(""), countryOfOrigin = None)
      searchForm.trimmedSearchTerm mustBe None
    }

    "must return None when searchTerm is only whitespace" in {
      val searchForm = SearchForm(searchTerm = Some("   "), countryOfOrigin = None)
      searchForm.trimmedSearchTerm mustBe None
    }

    "must return Some(trimmedValue) when searchTerm is a non-empty string" in {
      val searchForm = SearchForm(searchTerm = Some("hello"), countryOfOrigin = None)
      searchForm.trimmedSearchTerm mustBe Some("hello")
    }
  }
}
