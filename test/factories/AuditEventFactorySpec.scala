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

///*
// * Copyright 2024 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package factories
//
//import base.SpecBase
//import base.TestConstants.{testEori, testRecordId}
//import models.{Commodity, GoodsRecord, TraderProfile}
//import uk.gov.hmrc.auth.core.AffinityGroup
//import uk.gov.hmrc.http.HeaderCarrier
//
//import java.time.Instant
//
//class AuditEventFactorySpec extends SpecBase {
//  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
//
//  "audit event factory" - {
//
//    "create set up profile event" - {
//
//      "create event when all optionals supplied" in {
//
//        val traderProfile =
//          TraderProfile(testEori, "XIUKIM47699357400020231115081800", Some("RMS-GB-123456"), Some("612345"))
//
//        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "ProfileSetUp"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//        auditDetails.size mustBe 7
//        auditDetails("EORINumber") mustBe testEori
//        auditDetails("affinityGroup") mustBe "Individual"
//        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
//        auditDetails("isNIRMSRegistered") mustBe "true"
//        auditDetails("NIRMSNumber") mustBe "RMS-GB-123456"
//        auditDetails("isNIPHLRegistered") mustBe "true"
//        auditDetails("NIPHLNumber") mustBe "612345"
//
//      }
//
//      "create event when all optionals are not supplied" in {
//
//        val traderProfile = TraderProfile(testEori, "XIUKIM47699357400020231115081800", None, None)
//
//        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "ProfileSetUp"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//        auditDetails.size mustBe 5
//        auditDetails("EORINumber") mustBe testEori
//        auditDetails("affinityGroup") mustBe "Individual"
//        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
//        auditDetails("isNIRMSRegistered") mustBe "false"
//        auditDetails.get("NIRMSNumber") mustBe None
//        auditDetails("isNIPHLRegistered") mustBe "false"
//        auditDetails.get("NIPHLNumber") mustBe None
//
//      }
//
//    }
//
//    "create start create goods record" - {
//
//      "create event" in {
//
//        val result = AuditEventFactory().createStartCreateGoodsRecord(testEori, AffinityGroup.Individual)
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "StartCreateGoodsRecord"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//        auditDetails.size mustBe 2
//        auditDetails("EORINumber") mustBe testEori
//        auditDetails("affinityGroup") mustBe "Individual"
//
//      }
//
//    }
//
//    "create finish create goods record" - {
//
//      "create event when all optional fields populated" in {
//
//        val effectiveFrom = Instant.now
//        val effectiveTo   = effectiveFrom.plusSeconds(99)
//
//        val result = AuditEventFactory().createFinishCreateGoodsRecord(
//          AffinityGroup.Organisation,
//          GoodsRecord(
//            testEori,
//            "trader reference",
//            "030821",
//            "goods description",
//            "AG",
//            Instant.now,
//            Some(Instant.now)
//          ),
//          Commodity("030821", "Sea urchins", effectiveFrom, Some(effectiveTo)),
//          isUsingGoodsDescription = true
//        )
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "FinishCreateGoodsRecord"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//        auditDetails.size mustBe 10
//        auditDetails("EORINumber") mustBe testEori
//        auditDetails("affinityGroup") mustBe "Organisation"
//        auditDetails("traderReference") mustBe "trader reference"
//        auditDetails("specifiedGoodsDescription") mustBe "true"
//        auditDetails("goodsDescription") mustBe "goods description"
//        auditDetails("countryOfOrigin") mustBe "AG"
//        auditDetails("commodityDescription") mustBe "Sea urchins"
//        auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
//        auditDetails("commodityCodeEffectiveTo") mustBe effectiveTo.toString
//      }
//
//      "create event when optional fields are not defined" in {
//
//        val effectiveFrom = Instant.now
//
//        val result = AuditEventFactory().createFinishCreateGoodsRecord(
//          AffinityGroup.Organisation,
//          GoodsRecord(
//            testEori,
//            "trader reference",
//            "030821",
//            "trader reference",
//            "AG",
//            Instant.now,
//            Some(Instant.now)
//          ),
//          Commodity("030821", "Sea urchins", effectiveFrom, None),
//          isUsingGoodsDescription = false
//        )
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "FinishCreateGoodsRecord"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//        auditDetails.size mustBe 10
//        auditDetails("specifiedGoodsDescription") mustBe "false"
//        auditDetails("goodsDescription") mustBe "trader reference"
//        auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
//        auditDetails("commodityCodeEffectiveTo") mustBe "null"
//      }
//
//    }
//
//    "create validate commodity code event" - {
//
//      "create event" in {
//
//        val result = AuditEventFactory().createValidateCommodityCodeEvent(
//          testEori,
//          AffinityGroup.Individual,
//          "CreateRecord",
//          Some(testRecordId),
//          testCommodity.commodityCode,
//          Instant.parse("2024-06-03T15:19:18.399Z"),
//          Instant.parse("2024-06-03T15:19:20.399Z"),
//          true,
//          "OK",
//          200,
//          "null",
//          "meat",
//          None,
//          Instant.parse("2012-01-01T00:00:00Z")
//        )
//
//        result.auditSource mustBe "trader-goods-profiles-frontend"
//        result.auditType mustBe "ValidateCommodityCode"
//        result.tags.isEmpty mustBe false
//
//        val auditDetails = result.detail
//
//        auditDetails.size mustBe 14
//        auditDetails("eori") mustBe testEori
//        auditDetails("affinityGroup") mustBe "Individual"
//        auditDetails("journey") mustBe "CreateRecord"
//        auditDetails("recordId") mustBe testRecordId
//        auditDetails("commodityCode") mustBe testCommodity.commodityCode
//        auditDetails("requestDateTime") mustBe "2024-06-03T15:19:18.399Z"
//        auditDetails("responseDateTime") mustBe "2024-06-03T15:19:20.399Z"
//        auditDetails("outcome.commodityCodeStatus") mustBe "valid"
//        auditDetails("outcome.status") mustBe "OK"
//        auditDetails("outcome.statusCode") mustBe "200"
//        auditDetails("outcome.failureReason") mustBe "null"
//        auditDetails("commodityDescription") mustBe "meat"
//        auditDetails("commodityCodeEffectiveTo") mustBe "null"
//        auditDetails("commodityCodeEffectiveFrom") mustBe "2012-01-01T00:00:00Z"
//
//      }
//
//    }
//
//  }
//
//}
