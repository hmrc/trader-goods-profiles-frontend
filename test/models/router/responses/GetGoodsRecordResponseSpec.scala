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

package models.router.responses

import base.SpecBase
import java.time.Instant

class GetGoodsRecordResponseSpec extends SpecBase {

  ".statusForView" - {

    val testValues = Seq(
      "IMMI Ready",
      "Not Ready For IMMI",
      "Not Ready For Use"
    )

    val expectedValues = Seq(
      "IMMI ready",
      "Not ready for IMMI",
      "Not ready for use"
    )

    val baseModel = GetGoodsRecordResponse(
      recordId = "test",
      eori = "test",
      actorId = "test",
      traderRef = "test",
      comcode = "test",
      adviceStatus = "test",
      goodsDescription = "test",
      countryOfOrigin = "test",
      category = Some(3),
      measurementUnit = None,
      comcodeEffectiveFromDate = Instant.now,
      version = 1,
      active = true,
      toReview = false,
      declarable = "toReplace",
      createdDateTime = Instant.now,
      updatedDateTime = Instant.now
    )

    testValues.zip(expectedValues).foreach { case (testValue, expectedValue) =>
      s"should return '$expectedValue' when declarable is set to '$testValue'" in {
        val modelWithDeclarable = baseModel.copy(declarable = testValue)
        assert(modelWithDeclarable.statusForView == expectedValue)
      }
    }

  }

}


