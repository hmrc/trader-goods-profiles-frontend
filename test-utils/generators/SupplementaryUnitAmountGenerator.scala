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

  def doublesInRange(min: Double, max: Double): Gen[String] = {
    val numberGen = choose(min, max).map(formatDouble)
    numberGen
  }

  def formatDouble(value: Double): String = {
    val formatted   = f"$value%.6f"
    val parts       = formatted.split("\\.")
    val integerPart = parts(0)
    val decimalPart = if (parts.length > 1) "." + parts(1) else ""
    integerPart + decimalPart
  }
}
