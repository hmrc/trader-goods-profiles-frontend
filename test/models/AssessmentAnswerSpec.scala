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
import models.ott.Certificate
import play.api.i18n.Messages
import play.api.libs.json.{JsString, JsSuccess, Json}

class AssessmentAnswerSpec extends SpecBase {

  "AssessmentAnswer" - {

    "must serialise / deserialise to / from JSON for an exemption" in {

      val exemption = AssessmentAnswer.Exemption("true")
      val json      = Json.toJson[AssessmentAnswer](exemption)

      json mustEqual JsString("true")
      json.validate[AssessmentAnswer] mustEqual JsSuccess(exemption)
    }

    "must serialise / deserialise to / from JSON for no exemption" in {

      val json = Json.toJson[AssessmentAnswer](AssessmentAnswer.NoExemption)

      json mustEqual JsString("false")
      json.validate[AssessmentAnswer] mustEqual JsSuccess(AssessmentAnswer.NoExemption)
    }

    "must remove duplicate exemptions from the radio list if we've been sent duplicates from OTT" in {

      val exemption1 = Certificate("id1", "code1", "desc1")
      val exemption2 = Certificate("id2", "code2", "desc2")
      val exemptions = Seq(exemption1, exemption1, exemption2, exemption1)

      implicit val testMessages: Messages = messages(applicationBuilder(None).build())

      val result = AssessmentAnswer.radioOptions(exemptions)

      result.size mustBe 4
      result.count(x => x.value.contains("id1")) mustBe 1

    }
  }
}
