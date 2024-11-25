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
import models.DeclarableStatus.{ImmiReady, NotReadyForImmi, NotReadyForUse}
import play.api.libs.json._

class DeclarableStatusSpec extends SpecBase {

  "DeclarableStatus" - {
    "must deserialize from json" - {
      "when IMMI Ready" in {
        Json.fromJson[DeclarableStatus](JsString("IMMI Ready")) mustBe JsSuccess(ImmiReady)
      }

      "when Not Ready For IMMI" in {
        Json.fromJson[DeclarableStatus](JsString("Not Ready For IMMI")) mustBe JsSuccess(NotReadyForImmi)
      }

      "when Not Ready For Use" in {
        Json.fromJson[DeclarableStatus](JsString("Not Ready For Use")) mustBe JsSuccess(NotReadyForUse)
      }
    }

    "must serialize to json" - {
      "when IMMI Ready" in {
        Json.toJson(ImmiReady: DeclarableStatus) mustBe JsString("IMMI Ready")
      }

      "when Not Ready For IMMI" in {
        Json.toJson(NotReadyForImmi: DeclarableStatus) mustBe JsString("Not Ready For IMMI")
      }

      "when Not Ready For Use" in {
        Json.toJson(NotReadyForUse: DeclarableStatus) mustBe JsString("Not Ready For Use")
      }
    }
  }

}
