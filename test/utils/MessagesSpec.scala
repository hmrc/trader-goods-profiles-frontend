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

import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala}
import scala.util.Using

class MessagesSpec extends SpecBase {

  lazy val english: Map[String, String] = loadMessages("en")
  lazy val welsh: Map[String, String]   = loadMessages("cy")

  // Used to search for instances of keys in files to check for usage
  lazy val appFiles: List[java.nio.file.Path] =
    Files
      .walk(Paths.get("app"))
      .filter(Files.isRegularFile(_))
      .filter(path => path.toString.endsWith(".scala") || path.toString.endsWith(".scala.html"))
      .iterator()
      .asScala
      .toList

  // Loaded via file because the MessagesAPI sometimes adds some extra keys not present in the messages files
  private def loadMessages(language: String): Map[String, String] = {
    val messagesPath = Paths.get(s"conf/messages.$language")
    if (Files.exists(messagesPath)) {
      Using(Source.fromFile(messagesPath.toFile, "UTF-8")) { source =>
        source
          .getLines()
          .filterNot(_.trim.startsWith("#"))
          .filter(_.contains("="))
          .map { line =>
            val split = line.split("=", 2)
            split(0).trim -> split(1).trim
          }
          .toMap
      }.getOrElse(Map.empty)
    } else {
      Map.empty
    }
  }

  private def findUnusedKeys(keys: Set[String], files: List[java.nio.file.Path]): Set[String] =
    keys.filterNot { key =>
      files.exists { file =>
        val content = Files.readAllLines(file).asScala.mkString(" ")
        content.contains(key)
      }
    }

  // Currently prints instead of failing because further welsh may be added. Can't use log.warn because the logging level of tests is set to OFF.
  private def failAndShowKeys(message: String, keys: Set[String]): Unit =
    if (keys.nonEmpty) {
      val sortedKeys  = keys.toSeq.sortBy(_.charAt(0))
      val warningText = sortedKeys.foldLeft(s"$message (${keys.size}) = ") { (acc, key) =>
        acc + s"\n$key"
      }
      println("\n" + warningText + ("\n" * 2))
    }

  "english messages" - {
    "must be used somewhere in the code" in {
      val missingKeys = findUnusedKeys(english.keySet, appFiles)
      failAndShowKeys("Warning: There are unused English message keys in the codebase", missingKeys)
    }

    "must have a welsh translation" in {
      val missingWelshKeys = english.keySet.diff(welsh.keySet)
      failAndShowKeys("Warning: There are English message keys missing Welsh translations", missingWelshKeys)
    }
  }

  "welsh messages" - {
    "must be used somewhere in the code" in {
      val missingKeys = findUnusedKeys(welsh.keySet, appFiles)
      failAndShowKeys("Warning: There are unused Welsh message keys in the codebase", missingKeys)
    }

    "must have an english translation" in {
      val missingEnglishKeys = welsh.keySet.diff(english.keySet)
      failAndShowKeys("Warning: There are Welsh message keys missing English translations", missingEnglishKeys)
    }
  }
}
