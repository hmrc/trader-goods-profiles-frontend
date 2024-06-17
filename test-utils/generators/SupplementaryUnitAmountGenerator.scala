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
import org.scalacheck.Gen.choose

trait SupplementaryUnitAmountGenerator extends Generators {

  def doublesInRangeWithCommas(min: Double, max: Double): Gen[String] = {
    val numberGen = choose(min, max).map(formatDoubleWithCommas)
    numberGen
  }

  def formatDoubleWithCommas(value: Double): String = {
    val formatted   = f"$value%.6f" // Format with 6 decimal places
    val parts       = formatted.split("\\.")
    val integerPart = parts(0).reverse.grouped(3).mkString(",").reverse // Add commas every three digits
    val decimalPart = if (parts.length > 1) "." + parts(1) else "" // Append decimal part if present
    integerPart + decimalPart
  }

  def genDoublesperseString(gen: Gen[String], value: String, frequencyV: Int = 1, frequencyN: Int = 10): Gen[String] = {

    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield seq1.toSeq.zip(seq2).foldLeft("") {
      case (acc, (n, Some(v))) =>
        acc + n + v
      case (acc, (n, _))       =>
        acc + n
    }
  }
}
