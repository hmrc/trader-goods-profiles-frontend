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

package generators

import org.scalacheck.Gen

trait NiphlNumberGenerator extends Generators {

  def niphlAlphaNumericGenerator(letterCount: Int, numberCount: Int): Gen[String] = {

    val letter  = Gen.listOfN(letterCount, Gen.alphaChar).map(_.mkString)
    val numbers = Gen.listOfN(numberCount, Gen.numChar).map(_.mkString)

    for {
      letter  <- letter
      numbers <- numbers
    } yield s"$letter$numbers"
  }

  def niphlAlphaNumericWithSpacesGenerator(letterCount: Int, numberCount: Int): Gen[String] = {

    val letter  = Gen.listOfN(letterCount, Gen.alphaChar).map(_.mkString)
    val numbers = Gen.listOfN(numberCount, Gen.numChar).map(_.mkString)

    for {
      letter  <- letter
      numbers <- numbers
    } yield s"$letter $numbers"
  }

  def niphlNumericGenerator(min: Int, max: Int): Gen[String] =
    intsInRange(min, max)
}