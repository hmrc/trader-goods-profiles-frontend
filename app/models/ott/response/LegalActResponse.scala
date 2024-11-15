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

package models.ott.response

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Reads, __}

case class LegalActResponse(
  id: String,
  regulationUrl: String,
  description: String
) extends IncludedElement

object LegalActResponse {

  implicit lazy val reads: Reads[LegalActResponse] =
    (
      (__ \ "id").read[String] and
        (__ \ "attributes" \ "regulation_url").read[String] and
        (__ \ "attributes" \ "description").read[String]
    )(LegalActResponse.apply _)

}
