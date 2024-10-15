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

trait StatusCodeGenerators {
  val errorResponses4xx: Gen[Int] = Gen.chooseNum(400: Int, 499: Int)
  val errorResponses5xx: Gen[Int] = Gen.chooseNum(500: Int, 599: Int)
  val errorResponses: Gen[Int]    = Gen.oneOf(errorResponses4xx, errorResponses5xx)
}
