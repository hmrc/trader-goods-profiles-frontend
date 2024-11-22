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
import play.api.libs.json._
import models.AdviceStatus._

class AdviceStatusSpec extends SpecBase {

  "AdviceStatus" - {
    "must deserialize from json" - {
      "when Not Requested" in {
        Json.fromJson[AdviceStatus](JsString("Not Requested")) mustBe JsSuccess(NotRequested)
      }

      "when Requested" in {
        Json.fromJson[AdviceStatus](JsString("Requested")) mustBe JsSuccess(Requested)
      }

      "when In progress" in {
        Json.fromJson[AdviceStatus](JsString("In progress")) mustBe JsSuccess(InProgress)
      }

      "when Information Requested" in {
        Json.fromJson[AdviceStatus](JsString("Information Requested")) mustBe JsSuccess(InformationRequested)
      }

      "when Advice Provided" in {
        Json.fromJson[AdviceStatus](JsString("Advice Provided")) mustBe JsSuccess(AdviceReceived)
      }

      "when Advice not provided" in {
        Json.fromJson[AdviceStatus](JsString("Advice not provided")) mustBe JsSuccess(AdviceNotProvided)
      }

      "when Advice request withdrawn" in {
        Json.fromJson[AdviceStatus](JsString("Advice request withdrawn")) mustBe JsSuccess(AdviceRequestWithdrawn)
      }
    }

    "must serialize to json" - {
      "when Not Requested" in {
        Json.toJson(NotRequested: AdviceStatus)(AdviceStatus.writes) mustBe JsString("Not Requested")
      }

      "when Requested" in {
        Json.toJson(Requested: AdviceStatus) mustBe JsString("Requested")
      }

      "when In Progress" in {
        Json.toJson(InProgress: AdviceStatus) mustBe JsString("In progress")
      }

      "when Information Requested" in {
        Json.toJson(InformationRequested: AdviceStatus) mustBe JsString("Information Requested")
      }

      "when Advice Provided" in {
        Json.toJson(AdviceReceived: AdviceStatus) mustBe JsString("Advice Provided")
      }

      "when Advice Not Provided" in {
        Json.toJson(AdviceNotProvided: AdviceStatus) mustBe JsString("Advice not provided")
      }

      "when Advice Request Withdrawn" in {
        Json.toJson(AdviceRequestWithdrawn: AdviceStatus) mustBe JsString("Advice request withdrawn")
      }
    }
  }

}
