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

import play.api.i18n.{Lang, Messages}

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {

  private val dateFormatter     = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")

  private def localisedDateTimeFormatters(formatter: DateTimeFormatter): Map[String, DateTimeFormatter] = Map(
    "en" -> formatter,
    "cy" -> formatter.withLocale(new Locale("cy"))
  )

  def dateFormat()(implicit lang: Lang): DateTimeFormatter =
    localisedDateTimeFormatters(dateFormatter).getOrElse(lang.code, dateFormatter)

  def dateTimeFormat()(implicit lang: Lang): DateTimeFormatter =
    localisedDateTimeFormatters(dateTimeFormatter).getOrElse(lang.code, dateTimeFormatter)

  val dateTimeHintFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d M yyyy")

  def convertToDateTimeString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    val formattedDate       = instant.atZone(ZoneOffset.UTC).toLocalDateTime.format(dateTimeFormat())
    formattedDate.replace("AM", "am").replace("PM", "pm")
  }

  def convertToDateString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    instant.atZone(ZoneOffset.UTC).toLocalDate.format(dateFormat())
  }
}
