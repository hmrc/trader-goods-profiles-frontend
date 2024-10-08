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

object StringFieldRegex {
  val ukimsNumberRegex: String                    = "^(GB|XI)UKIM[0-9]{12}[0-9]{14}$"
  val nirmsRegex: String                          = "RMS(GB|NI)[0-9]{6}"
  val niphlRegex: String                          = "^([0-9]{4,6}|[a-zA-Z]{1,2}[0-9]{5})$"
  val commodityCodeFormatRegex: String            = "^([0-9]{6}|[0-9]{8}|[0-9]{10})$"
  val emailRegex: String                          = """^\w+([-+.']\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$"""
  val commodityCodeAdditionalNumbersRegex: String = "^([0-9]{2}|[0-9]{4})$"
  val supplementaryUnitRegex                      = """^-?\d{1,10}(\.\d{1,6})?$"""
}
