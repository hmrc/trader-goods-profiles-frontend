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

package factories

import base.SpecBase
import base.TestConstants.testEori
import models.TraderProfile
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

class AuditEventFactorySpec extends SpecBase {
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "audit event factory" - {

    "create set up profile event" - {

      "create event when all optionals supplied" in {

        val traderProfile =
          TraderProfile(testEori, testEori, "XIUKIM47699357400020231115081800", Some("RMS-GB-123456"), Some("612345"))

        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "ProfileSetUp"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 7
        auditDetails("EORINumber") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
        auditDetails("isNIRMSRegistered") mustBe "true"
        auditDetails("NIRMSNumber") mustBe "RMS-GB-123456"
        auditDetails("isNIPHLRegistered") mustBe "true"
        auditDetails("NIPHLNumber") mustBe "612345"

      }

      "create event when all optionals are not supplied" in {

        val traderProfile = TraderProfile(testEori, testEori, "XIUKIM47699357400020231115081800", None, None)

        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "ProfileSetUp"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 5
        auditDetails("EORINumber") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
        auditDetails("isNIRMSRegistered") mustBe "false"
        auditDetails.get("NIRMSNumber") mustBe None
        auditDetails("isNIPHLRegistered") mustBe "false"
        auditDetails.get("NIPHLNumber") mustBe None

      }

    }

  }

}
