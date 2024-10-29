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

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SeqOpsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SeqOps" - {
    ".filterOption" - {
      "must return None when filter returns List()" in {
        val seq: Seq[Boolean] = Seq.empty

        val result = seq.filterToOption(_ => true)

        result mustBe None
      }

      "must return Some(predicate) when filter returns something" in {
        val seq = Seq(true)

        val result = seq.filterToOption(_ => true)

        result mustBe Some(Seq(true))
      }
    }
  }
}
