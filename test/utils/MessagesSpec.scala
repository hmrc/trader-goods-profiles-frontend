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
import play.api.i18n.MessagesApi

import scala.util.matching.Regex

class MessagesSpec extends SpecBase {

  lazy val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]

  val matchSingleQuoteOnly: Regex   = """'+""".r
  val matchBacktickQuoteOnly: Regex = """`+""".r

  private val commonMessageKeys = Set(
    "service.name",
    "site.back",
    "site.remove",
    "site.change",
    "site.no",
    "site.yes",
    "site.continue",
    "site.start",
    "site.startAgain",
    "site.signIn",
    "site.govuk",
    "date.day",
    "date.month",
    "date.year",
    "date.error.day",
    "date.error.month",
    "date.error.year",
    "timeout.title",
    "timeout.message",
    "timeout.keepAlive",
    "timeout.signOut",
    "error.prefix",
    "error.boolean",
    "error.invalid_date",
    "error.date.day_blank",
    "error.date.day_invalid",
    "error.date.month_blank",
    "error.date.month_invalid",
    "error.date.year_blank",
    "error.date.year_invalid",
    "error.integer",
    "error.non_numeric",
    "error.number",
    "error.required",
    "error.summary.title",
    "signedOut.title",
    "signedOut.heading",
    "signedOut.guidance",
    "unauthorised.title",
    "unauthorised.heading",
    "unauthorised.p1",
    "unauthorised.p2",
    "unauthorised.p2.linkText"
  )

  private val serviceMessageKeys = Set(
    "categoryGuidance.title",
    "categoryGuidance.h1",
    "categoryGuidance.p1",
    "categoryGuidance.p2",
    "categoryGuidance.p3",
    "categoryGuidance.p4",
    "categoryGuidance.p5",
    "profileSetup.title",
    "profileSetup.h1",
    "profileSetup.body.intro",
    "profileSetup.ukims.h2",
    "profileSetup.p1",
    "profileSetup.p2",
    "profileSetup.p3",
    "profileSetup.p3.linkText",
    "profileSetup.nirms.h2",
    "profileSetup.p4",
    "profileSetup.p5",
    "profileSetup.p6",
    "profileSetup.p6.linkText",
    "profileSetup.niphl.h2",
    "profileSetup.p7",
    "profileSetup.p8",
    "profileSetup.p9",
    "profileSetup.p9.linkText",
    "niphlQuestion.title",
    "niphlQuestion.h1",
    "niphlQuestion.p1",
    "niphlQuestion.p2",
    "niphlQuestion.p2.linkText",
    "niphlQuestion.h2",
    "niphlQuestion.radio.notSelected",
    "ukimsNumber.title",
    "ukimsNumber.h1",
    "ukimsNumber.p1",
    "ukimsNumber.p2",
    "ukimsNumber.heading",
    "ukimsNumber.hint",
    "ukimsNumber.linkText",
    "ukimsNumber.error.required",
    "ukimsNumber.error.invalidFormat",
    "nirmsQuestion.title",
    "nirmsQuestion.h1",
    "nirmsQuestion.p1",
    "nirmsQuestion.p2",
    "nirmsQuestion.p2.linkText",
    "nirmsQuestion.h2",
    "nirmsQuestion.error.notSelected",
    "nirmsNumber.title",
    "nirmsNumber.heading",
    "nirmsNumber.hint",
    "nirmsNumber.error.required",
    "nirmsNumber.error.invalidFormat",
    "niphlNumber.title",
    "niphlNumber.h1",
    "niphlNumber.p1",
    "niphlNumber.li1",
    "niphlNumber.li2",
    "niphlNumber.li3",
    "niphlNumber.h2",
    "niphlNumber.hint",
    "niphlNumber.error.notSupplied",
    "niphlNumber.error.wrongFormat",
    "checkYourAnswers.title",
    "checkYourAnswers.heading",
    "journeyRecovery.continue.title",
    "journeyRecovery.continue.heading",
    "journeyRecovery.continue.guidance",
    "journeyRecovery.startAgain.title",
    "journeyRecovery.startAgain.heading",
    "journeyRecovery.startAgain.guidance"
  )

  "All message files" - {
    "must have a non-empty message for each key" in {
      assertNonEmptyNonTemporaryValues("en", englishMessages)
    }
    "must have no unescaped single quotes in value" in {
      assertCorrectUseOfQuotes("en", englishMessages)
    }
    "must not have missing keys" in {
      assertNoMissingKeys("en", englishMessages, serviceMessageKeys)
    }

  }

  private def assertNonEmptyNonTemporaryValues(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (key, value) =>
      withClue(s"In $label, there is an empty value for the key:[$key][$value]") {
        value.trim.nonEmpty mustBe true
      }
  }

  private def assertCorrectUseOfQuotes(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (key, value) =>
      withClue(s"In $label, there is an unescaped or invalid quote:[$key][$value]") {
        matchSingleQuoteOnly.findFirstIn(value).isEmpty mustBe true
        matchBacktickQuoteOnly.findFirstIn(value).isEmpty mustBe true
      }
  }

  private def assertNoMissingKeys(label: String, messages: Map[String, String], expectedKeys: Set[String]): Unit = {
    val missingKeys = expectedKeys.diff(messages.keySet)
    withClue(s"In $label, the following keys are missing: ${missingKeys.mkString(", ")}") {
      missingKeys.isEmpty mustBe true
    }
  }

  private lazy val englishMessages: Map[String, String] = getExpectedMessages("en") -- commonMessageKeys

  private def getExpectedMessages(languageCode: String) =
    messagesAPI.messages.getOrElse(languageCode, throw new Exception(s"Missing messages for $languageCode"))
}
