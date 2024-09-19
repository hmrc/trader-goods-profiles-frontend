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

package forms.mappings.helpers

object FormatAnswers {

  def trimAndCompressSpaces(original: String): String                = original.trim.replaceAll("\\s+", " ")
  def removeWhitespace(original: String): String                     = original.filterNot(_.isWhitespace)
  def toUppercaseAndRemoveSpacesAndHyphens(original: String): String =
    original.toUpperCase.replaceAll(" ", "").replaceAll("-", "")
  def addHyphensToNirms(original: String): String = {
    val hyphenPos1 = 3
    val hyphenPos2 = 5
    val bite1      = original.slice(0, hyphenPos1)
    val bite2      = original.slice(hyphenPos1, hyphenPos2)
    val bite3      = original.slice(hyphenPos2, original.length)
    s"$bite1-$bite2-$bite3"
  }
}
