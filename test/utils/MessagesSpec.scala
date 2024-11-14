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
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._

class MessagesSpec extends SpecBase {

  lazy val messages = applicationBuilder().build().injector.instanceOf[MessagesApi].messages
  lazy val english = messages("en")
  lazy val welsh = messages("cy")

  "messages" - {

    "must be used somewhere in the code" in {
      val appPath = "/Users/Ewan.Donovan/git/git3/trader-goods-profiles-frontend/app"

      val allFiles = Files.walk(Paths.get(appPath))
        .filter(Files.isRegularFile(_))
        .filter(path => path.toString.endsWith(".scala") || path.toString.endsWith(".scala.html"))
        .iterator()
        .asScala
        .toList

      val missingKeys = english.keySet.filterNot { key =>
        allFiles.exists { file =>
          val content = Files.readAllLines(file).asScala.mkString(" ")
          content.contains(key)
        }
      }

      if (missingKeys.nonEmpty) {
        val warningText = missingKeys.foldLeft(
          s"Warning: There are ${missingKeys.size} unused message keys in the codebase:"
        ) { case (warningString, key) =>
          warningString + s"\n$key: ${english(key)}"
        }

        println("\n" + warningText + ("\n"*2))
      }
    }


    "must have a welsh translation" in {
      val missingWelshKeys = english.keySet.filterNot(welsh.keySet)

      if (missingWelshKeys.nonEmpty) {
        val failureText = missingWelshKeys.foldLeft(
          s"Warning: There are ${missingWelshKeys.size} message keys missing Welsh translations:"
        ) { case (failureString, key) =>
          failureString + s"\n$key: ${english(key)}"
        }

        println("\n" + failureText + ("\n"*2))
      }
    }

  }
}