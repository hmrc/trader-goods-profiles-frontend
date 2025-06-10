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

package utils

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{Lang, Messages}
import utils.DateTimeFormats.dateFormat

import java.time.{Instant, LocalDate}

class DateTimeFormatsSpec extends SpecBase with Matchers {

  ".dateTimeFormat" - {
    "must format dates in English" in {
      val formatter = dateFormat()(Lang("en"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }

    "must format dates in Welsh" in {
      val formatter = dateFormat()(Lang("cy"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Ionawr 2023"
    }

    "must default to English format" in {
      val formatter = dateFormat()(Lang("de"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }
  }

  "convertToDateTimeString" - {
    "must return correct date time string when am" in {
      val application                     = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      implicit val messagesImpl: Messages = messages(application)

      val instant = Instant.parse("2024-04-22T10:05:00Z")
      DateTimeFormats.convertToDateTimeString(instant) mustEqual "22 April 2024 10:05am"
    }

    "must return correct date time string when pm" in {
      val application                     = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      implicit val messagesImpl: Messages = messages(application)

      val instant = Instant.parse("2024-04-22T13:11:00Z")
      DateTimeFormats.convertToDateTimeString(instant) mustEqual "22 April 2024 1:11pm"
    }
  }
}
