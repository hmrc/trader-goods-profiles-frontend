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

import base.SpecBase
import play.api.libs.json.{JsString, JsSuccess, Json}

class AssessmentAnswerSpec extends SpecBase {

  "AssessmentAnswer" - {

    "must serialise / deserialise to / from JSON" - {
      "for an exemption" in {

        val exemption = AssessmentAnswer.Exemption(Seq("TEST_CODE"))
        val json      = Json.toJson[AssessmentAnswer](exemption)

        json mustEqual JsString("true")
        json.validate[AssessmentAnswer] mustEqual JsSuccess(exemption)
      }

      "for no exemption" in {

        val json = Json.toJson[AssessmentAnswer](AssessmentAnswer.NoExemption)

        json mustEqual JsString("false")
        json.validate[AssessmentAnswer] mustEqual JsSuccess(AssessmentAnswer.NoExemption)
      }

      "for unanswered reassessment questions" in {

        val json = Json.toJson[AssessmentAnswer](AssessmentAnswer.NotAnsweredYet)

        json mustEqual JsString("notAnswered")
        json.validate[AssessmentAnswer] mustEqual JsSuccess(AssessmentAnswer.NotAnsweredYet)
      }
    }
  }
}
