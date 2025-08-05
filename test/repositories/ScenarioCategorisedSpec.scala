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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.{Lang, Messages}

import java.util.Locale

trait ScenarioCategorised {
  def messageKey: String
  def toString(implicit messages: Messages): String = messages(messageKey)
}

case object DummyScenario extends ScenarioCategorised {
  override def messageKey: String = "test.key"
}

class ScenarioCategorisedSpec extends AnyFreeSpec with Matchers {

  val testMessages: Messages = new Messages {

    override def apply(key: String, args: Seq[Any]): String = key

    override def apply(keys: Seq[String], args: Seq[Any]): String = keys.headOption.getOrElse("")

    override def asJava: play.i18n.Messages =
      throw new UnsupportedOperationException("asJava not implemented in test stub")

    override def isDefinedAt(key: String): Boolean = true

    override def translate(key: String, args: Seq[Any]): Option[String] = Some(key)

    override def lang: Lang = Lang(Locale.forLanguageTag("en"))
  }

  "ScenarioCategorised" - {

    "toString should return the messageKey from Messages" in {
      implicit val messages: Messages = testMessages

      DummyScenario.toString shouldBe "test.key"
    }
  }
}
