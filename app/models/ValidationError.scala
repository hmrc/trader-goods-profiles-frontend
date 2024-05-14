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

import queries.Query

sealed trait ValidationError {

  val query: Query
  val message: String
}

final case class PageMissing(query: Query) extends ValidationError {

  val message: String = s"Page missing: ${query.path}"
}

final case class UnexpectedPage(query: Query) extends ValidationError {

  val message: String = s"Unexpected page: ${query.path}"
}
