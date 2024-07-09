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
import base.TestConstants.{testEori, testRecordId}
import models.audits.{AuditGetCategorisationAssessment, AuditValidateCommodityCode, GetCategorisationAssessmentDetailsEvent, OttAuditData, ValidateCommodityCodeEvent}
import models.helper.{CategorisationUpdate, CreateRecordJourney, UpdateRecordJourney}
import models.ott.response._
import models.{Commodity, GoodsRecord, TraderProfile}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}

class AuditEventFactorySpec extends SpecBase {
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "audit event factory" - {

    "create set up profile event" - {

      "create event when all optionals supplied" in {

        val traderProfile =
          TraderProfile(testEori, "XIUKIM47699357400020231115081800", Some("RMS-GB-123456"), Some("612345"))

        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "ProfileSetUp"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 7
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
        auditDetails("NIRMSRegistered") mustBe "true"
        auditDetails("NIRMSNumber") mustBe "RMS-GB-123456"
        auditDetails("NIPHLRegistered") mustBe "true"
        auditDetails("NIPHLNumber") mustBe "612345"

      }

      "create event when all optionals are not supplied" in {

        val traderProfile = TraderProfile(testEori, "XIUKIM47699357400020231115081800", None, None)

        val result = AuditEventFactory().createSetUpProfileEvent(traderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "ProfileSetUp"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 5
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("UKIMSNumber") mustBe "XIUKIM47699357400020231115081800"
        auditDetails("NIRMSRegistered") mustBe "false"
        auditDetails.get("NIRMSNumber") mustBe None
        auditDetails("NIPHLRegistered") mustBe "false"
        auditDetails.get("NIPHLNumber") mustBe None

      }

    }

    "create start manage goods record event" - {

      "create event when journey is create record" in {

        val result = AuditEventFactory()
          .createStartManageGoodsRecordEvent(testEori, AffinityGroup.Individual, CreateRecordJourney, None, None)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "StartManageGoodsRecord"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 3
        auditDetails("journey") mustBe "CreateRecord"
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"

      }

      "create event when journey is update record" in {

        val result = AuditEventFactory().createStartManageGoodsRecordEvent(
          testEori,
          AffinityGroup.Individual,
          UpdateRecordJourney,
          Some(CategorisationUpdate),
          Some("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f")
        )

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "StartManageGoodsRecord"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 5
        auditDetails("journey") mustBe "UpdateRecord"
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("updateSection") mustBe "categorisation"
        auditDetails("recordId") mustBe "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

      }

    }

    "create submit goods record event" - {

      "create event when journey is creating a goods record" - {

        "and specified goods description and there is an effective-to date" in {

          val effectiveFrom = Instant.now
          val effectiveTo   = effectiveFrom.plusSeconds(99)
          val commodity     =
            Commodity(
              "030821",
              List(
                "Sea urchins",
                "Live, fresh or chilled",
                "Aquatic invertebrates other than crustaceans and molluscs "
              ),
              effectiveFrom,
              Some(effectiveTo)
            )

          val result = AuditEventFactory().createSubmitGoodsRecordEventForCreateRecord(
            AffinityGroup.Organisation,
            CreateRecordJourney,
            GoodsRecord(
              testEori,
              "trader reference",
              commodity,
              "goods description",
              "AG"
            ),
            isUsingGoodsDescription = true
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 11
          auditDetails("journey") mustBe "CreateRecord"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails("traderReference") mustBe "trader reference"
          auditDetails("specifiedGoodsDescription") mustBe "true"
          auditDetails("goodsDescription") mustBe "goods description"
          auditDetails("countryOfOrigin") mustBe "AG"
          auditDetails("commodityCode") mustBe "030821"
          auditDetails("commodityDescription") mustBe "Sea urchins"
          auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
          auditDetails("commodityCodeEffectiveTo") mustBe effectiveTo.toString

        }

        "and not specified goods description" in {

          val effectiveFrom = Instant.now
          val commodity     = Commodity(
            "030821",
            List(
              "Sea urchins",
              "Live, fresh or chilled",
              "Aquatic invertebrates other than crustaceans and molluscs "
            ),
            effectiveFrom,
            None
          )

          val result = AuditEventFactory().createSubmitGoodsRecordEventForCreateRecord(
            AffinityGroup.Organisation,
            CreateRecordJourney,
            GoodsRecord(
              testEori,
              "trader reference",
              commodity,
              "trader reference",
              "AG"
            ),
            isUsingGoodsDescription = false
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 11
          auditDetails("journey") mustBe "CreateRecord"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails("traderReference") mustBe "trader reference"
          auditDetails("specifiedGoodsDescription") mustBe "false"
          auditDetails("goodsDescription") mustBe "trader reference"
          auditDetails("countryOfOrigin") mustBe "AG"
          auditDetails("commodityCode") mustBe "030821"
          auditDetails("commodityDescription") mustBe "Sea urchins"
          auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
          auditDetails("commodityCodeEffectiveTo") mustBe "null"

        }

      }

      "create event when journey is updating a goods record" - {

        "and update is for categorisation" in {

          val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
            testEori,
            AffinityGroup.Organisation,
            UpdateRecordJourney,
            testRecordId,
            2,
            1
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 7
          auditDetails("journey") mustBe "UpdateRecord"
          auditDetails("updateSection") mustBe "categorisation"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails("recordId") mustBe testRecordId
          auditDetails("categoryAssessmentsWithExemptions") mustBe "2"
          auditDetails("category") mustBe "1"
        }

        "and update is for goods details" in {

          val effectiveFrom = Instant.now
          val effectiveTo   = effectiveFrom.plusSeconds(99)
          val commodity     = Commodity(
            "030821",
            List(
              "Sea urchins",
              "Live, fresh or chilled",
              "Aquatic invertebrates other than crustaceans and molluscs "
            ),
            effectiveFrom,
            Some(effectiveTo)
          )

          val result = AuditEventFactory().createSubmitGoodsRecordEventForUpdateRecord(
            AffinityGroup.Organisation,
            CreateRecordJourney,
            GoodsRecord(
              testEori,
              "trader reference",
              commodity,
              "goods description",
              "AG"
            )
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 11
          auditDetails("journey") mustBe "CreateRecord"
          auditDetails("updateSection") mustBe "goodsDetails"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails("traderReference") mustBe "trader reference"
          auditDetails("goodsDescription") mustBe "goods description"
          auditDetails("countryOfOrigin") mustBe "AG"
          auditDetails("commodityCode") mustBe "030821"
          auditDetails("commodityDescription") mustBe "Sea urchins"
          auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
          auditDetails("commodityCodeEffectiveTo") mustBe effectiveTo.toString

        }

      }

    }

    "create validate commodity code event" - {

      "create event" - {

        "when all is valid in CreateRecord journey" in {

          val auditData = OttAuditData(
            AuditValidateCommodityCode,
            testEori,
            AffinityGroup.Individual,
            None,
            testCommodity.commodityCode,
            None,
            None,
            Some(CreateRecordJourney)
          )

          val result = AuditEventFactory().createValidateCommodityCodeEvent(
            auditData,
            Instant.parse("2024-06-03T15:19:18.399Z"),
            Instant.parse("2024-06-03T15:19:20.399Z"),
            OK,
            None,
            Some(testAuditOttResponse)
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "ValidateCommodityCode"
          result.tags.isEmpty mustBe false

          val auditDetails = Json.fromJson[ValidateCommodityCodeEvent](result.detail).get
          auditDetails.eori mustBe testEori
          auditDetails.affinityGroup mustBe "Individual"
          auditDetails.journey mustBe Some("CreateRecord")
          auditDetails.recordId mustBe None
          auditDetails.commodityCode mustBe "1234567890"
          auditDetails.requestDateTime mustBe "2024-06-03T15:19:18.399Z"
          auditDetails.responseDateTime mustBe "2024-06-03T15:19:20.399Z"
          auditDetails.outcome.commodityCodeStatus mustBe "valid"
          auditDetails.outcome.status mustBe "OK"
          auditDetails.outcome.statusCode mustBe "200"
          auditDetails.outcome.failureReason mustBe None
          auditDetails.commodityDescription mustBe Some("test")
          auditDetails.commodityCodeEffectiveTo mustBe Some("null")
          auditDetails.commodityCodeEffectiveFrom mustBe Some("1970-01-01T00:00:00Z")

        }

        "when all is valid in UpdateRecord journey" in {

          val auditData = OttAuditData(
            AuditValidateCommodityCode,
            testEori,
            AffinityGroup.Individual,
            Some(testRecordId),
            testCommodity.commodityCode,
            None,
            None,
            Some(UpdateRecordJourney)
          )

          val goodsNomenclatureWithExpiryDate =
            testAuditOttResponse.goodsNomenclature.copy(validityEndDate =
              Some(Instant.parse("2024-07-31T23:59:59.999Z"))
            )

          val testAuditOttResponseWithExpiryDate =
            testAuditOttResponse.copy(goodsNomenclature = goodsNomenclatureWithExpiryDate)

          val result = AuditEventFactory().createValidateCommodityCodeEvent(
            auditData,
            Instant.parse("2024-06-03T15:19:18.399Z"),
            Instant.parse("2024-06-03T15:19:20.399Z"),
            OK,
            None,
            Some(testAuditOttResponseWithExpiryDate)
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "ValidateCommodityCode"
          result.tags.isEmpty mustBe false

          val auditDetails = Json.fromJson[ValidateCommodityCodeEvent](result.detail).get
          auditDetails.eori mustBe testEori
          auditDetails.affinityGroup mustBe "Individual"
          auditDetails.journey mustBe Some("UpdateRecord")
          auditDetails.recordId mustBe Some(testRecordId)
          auditDetails.commodityCode mustBe "1234567890"
          auditDetails.requestDateTime mustBe "2024-06-03T15:19:18.399Z"
          auditDetails.responseDateTime mustBe "2024-06-03T15:19:20.399Z"
          auditDetails.outcome.commodityCodeStatus mustBe "valid"
          auditDetails.outcome.status mustBe "OK"
          auditDetails.outcome.statusCode mustBe "200"
          auditDetails.outcome.failureReason mustBe None
          auditDetails.commodityDescription mustBe Some("test")
          auditDetails.commodityCodeEffectiveTo mustBe Some("2024-07-31T23:59:59.999Z")
          auditDetails.commodityCodeEffectiveFrom mustBe Some("1970-01-01T00:00:00Z")

        }

        "when invalid commodity code" in {

          val auditData = OttAuditData(
            AuditValidateCommodityCode,
            testEori,
            AffinityGroup.Individual,
            Some(testRecordId),
            testCommodity.commodityCode,
            None,
            None,
            Some(UpdateRecordJourney)
          )

          val result = AuditEventFactory().createValidateCommodityCodeEvent(
            auditData,
            Instant.parse("2024-06-03T15:19:18.399Z"),
            Instant.parse("2024-06-03T15:19:20.399Z"),
            NOT_FOUND,
            Some("Commodity not valid"),
            None
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "ValidateCommodityCode"
          result.tags.isEmpty mustBe false

          val auditDetails = Json.fromJson[ValidateCommodityCodeEvent](result.detail).get
          auditDetails.eori mustBe testEori
          auditDetails.affinityGroup mustBe "Individual"
          auditDetails.journey mustBe Some("UpdateRecord")
          auditDetails.recordId mustBe Some(testRecordId)
          auditDetails.commodityCode mustBe "1234567890"
          auditDetails.requestDateTime mustBe "2024-06-03T15:19:18.399Z"
          auditDetails.responseDateTime mustBe "2024-06-03T15:19:20.399Z"
          auditDetails.outcome.commodityCodeStatus mustBe "invalid"
          auditDetails.outcome.status mustBe "Not Found"
          auditDetails.outcome.statusCode mustBe "404"
          auditDetails.outcome.failureReason mustBe Some("Commodity not valid")
          auditDetails.commodityDescription mustBe None
          auditDetails.commodityCodeEffectiveTo mustBe None
          auditDetails.commodityCodeEffectiveFrom mustBe None

        }

      }
    }

    "create get categorisation assessment details event" - {

      "create event" - {

        "when all is valid" in {

          val auditData = OttAuditData(
            AuditGetCategorisationAssessment,
            testEori,
            AffinityGroup.Individual,
            Some(testRecordId),
            testCommodity.commodityCode,
            Some("US"),
            Some(LocalDate.of(2024, 6, 18)),
            None
          )

          val ottResponse = OttResponse(
            goodsNomenclature = GoodsNomenclatureResponse("id", "commodity code", None, Instant.EPOCH, None, "test"),
            categoryAssessmentRelationships = Seq(
              CategoryAssessmentRelationship("assessmentId1"),
              CategoryAssessmentRelationship("assessmentId2")
            ),
            includedElements = Seq(
              CategoryAssessmentResponse("assessmentId1", "themeId1", Nil),
              ThemeResponse("themeId1", 1),
              CategoryAssessmentResponse(
                "assessmentId2",
                "themeId2",
                Seq(
                  ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                  ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                )
              ),
              ThemeResponse("themeId2", 2),
              CertificateResponse("exemptionId1", "code1", "description1"),
              AdditionalCodeResponse("exemptionId2", "code2", "description2"),
              ThemeResponse("ignoredTheme", 3),
              CertificateResponse("ignoredExemption", "code3", "description3")
            ),
            descendents = Seq.empty[Descendant]
          )

          val result = AuditEventFactory().createGetCategorisationAssessmentDetailsEvent(
            auditData,
            Instant.parse("2024-06-03T15:19:18.399Z"),
            Instant.parse("2024-06-03T15:19:20.399Z"),
            OK,
            None,
            Some(ottResponse)
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "GetCategorisationAssessmentDetails"
          result.tags.isEmpty mustBe false

          val auditDetails = Json.fromJson[GetCategorisationAssessmentDetailsEvent](result.detail).get
          auditDetails.eori mustBe testEori
          auditDetails.affinityGroup mustBe "Individual"
          auditDetails.recordId mustBe Some(testRecordId)
          auditDetails.commodityCode mustBe "1234567890"
          auditDetails.countryOfOrigin mustBe Some("US")
          auditDetails.dateOfTrade mustBe Some("2024-06-18")
          auditDetails.requestDateTime mustBe "2024-06-03T15:19:18.399Z"
          auditDetails.responseDateTime mustBe "2024-06-03T15:19:20.399Z"
          auditDetails.outcome.status mustBe "OK"
          auditDetails.outcome.statusCode mustBe "200"
          auditDetails.outcome.failureReason mustBe None
          auditDetails.categoryAssessmentOptions mustBe Some("2")
          auditDetails.exemptionOptions mustBe Some("2")

        }

        "when lookup fails" in {

          val auditData = OttAuditData(
            AuditGetCategorisationAssessment,
            testEori,
            AffinityGroup.Individual,
            Some(testRecordId),
            testCommodity.commodityCode,
            Some("US"),
            Some(LocalDate.of(2024, 6, 18)),
            None
          )

          val result = AuditEventFactory().createGetCategorisationAssessmentDetailsEvent(
            auditData,
            Instant.parse("2024-06-03T15:19:18.399Z"),
            Instant.parse("2024-06-03T15:19:20.399Z"),
            NOT_FOUND,
            Some("Commodity not valid"),
            None
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "GetCategorisationAssessmentDetails"
          result.tags.isEmpty mustBe false

          val auditDetails = Json.fromJson[GetCategorisationAssessmentDetailsEvent](result.detail).get
          auditDetails.eori mustBe testEori
          auditDetails.affinityGroup mustBe "Individual"
          auditDetails.recordId mustBe Some(testRecordId)
          auditDetails.commodityCode mustBe "1234567890"
          auditDetails.countryOfOrigin mustBe Some("US")
          auditDetails.dateOfTrade mustBe Some("2024-06-18")
          auditDetails.requestDateTime mustBe "2024-06-03T15:19:18.399Z"
          auditDetails.responseDateTime mustBe "2024-06-03T15:19:20.399Z"
          auditDetails.outcome.status mustBe "Not Found"
          auditDetails.outcome.statusCode mustBe "404"
          auditDetails.outcome.failureReason mustBe Some("Commodity not valid")
          auditDetails.categoryAssessmentOptions mustBe None
          auditDetails.exemptionOptions mustBe None
        }

      }
    }

  }

}
