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
import models.audits._
import models.helper.{CategorisationUpdate, CreateRecordJourney, RequestAdviceJourney, UpdateRecordJourney}
import models.ott.response._
import models.{AdviceRequest, Category1Scenario, Category2Scenario, CategoryRecord, Commodity, GoodsRecord, SupplementaryRequest, TraderProfile, UpdateGoodsRecord}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.{commodityCodeKey, countryOfOriginKey, goodsDescriptionKey, traderReferenceKey}

import java.time.{Instant, LocalDate}

class AuditEventFactorySpec extends SpecBase {
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "audit event factory" - {

    "create set up profile event" - {

      "create event when all optionals supplied" in {

        val traderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123456"),
            Some("612345"),
            eoriChanged = false
          )

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

        val traderProfile = TraderProfile(testEori, "XIUKIM47699357400020231115081800", None, None, eoriChanged = false)

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

        val catInfo = categorisationInfo.copy(categoryAssessmentsThatNeedAnswers = Seq(category1, category3))

        val result = AuditEventFactory().createStartManageGoodsRecordEvent(
          testEori,
          AffinityGroup.Individual,
          UpdateRecordJourney,
          Some(CategorisationUpdate),
          Some("8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"),
          Some(catInfo)
        )

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "StartManageGoodsRecord"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 9
        auditDetails("journey") mustBe "UpdateRecord"
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("updateSection") mustBe "categorisation"
        auditDetails("recordId") mustBe "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
        auditDetails(commodityCodeKey) mustBe "1234567890"
        auditDetails(countryOfOriginKey) mustBe "BV"
        auditDetails("descendants") mustBe "1"
        auditDetails("categoryAssessments") mustBe "2"
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
            )
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 10
          auditDetails("journey") mustBe "CreateRecord"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails(traderReferenceKey) mustBe "trader reference"
          auditDetails(goodsDescriptionKey) mustBe "goods description"
          auditDetails(countryOfOriginKey) mustBe "AG"
          auditDetails(commodityCodeKey) mustBe "030821"
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
              "DESCRIPTION",
              "AG"
            )
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 10
          auditDetails("journey") mustBe "CreateRecord"
          auditDetails("eori") mustBe testEori
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails(traderReferenceKey) mustBe "trader reference"
          auditDetails(goodsDescriptionKey) mustBe "DESCRIPTION"
          auditDetails(countryOfOriginKey) mustBe "AG"
          auditDetails(commodityCodeKey) mustBe "030821"
          auditDetails("commodityDescription") mustBe "Sea urchins"
          auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
          auditDetails("commodityCodeEffectiveTo") mustBe "null"

        }
      }

      "create event when journey is updating a goods record" - {

        "and update is for categorisation" - {

          "without longer commodity code journey nor supplementary unit" in {

            val categoryInfo = categorisationInfo.copy(
              categoryAssessmentsThatNeedAnswers = Seq(category1, category3)
            )

            val categoryRecord = CategoryRecord(
              testEori,
              testRecordId,
              "1234567890",
              Category1Scenario,
              Some("Weight, in kilograms"),
              None,
              categoryInfo,
              2,
              wasSupplementaryUnitAsked = false
            )

            val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
              testEori,
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              testRecordId,
              categoryRecord
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 12
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "categorisation"
            auditDetails("eori") mustBe testEori
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("recordId") mustBe testRecordId
            auditDetails(commodityCodeKey) mustBe "1234567890"
            auditDetails(countryOfOriginKey) mustBe "BV"
            auditDetails("descendants") mustBe "1"
            auditDetails("categoryAssessments") mustBe "2"
            auditDetails("categoryAssessmentsAnswered") mustBe "2"
            auditDetails("reassessmentNeeded") mustBe "false"
            auditDetails("category") mustBe "1"
          }

          "with supplementary unit asked but not supplied" in {

            val categoryRecord = CategoryRecord(
              testEori,
              testRecordId,
              "1234567890",
              Category1Scenario,
              Some("Weight, in kilograms"),
              None,
              categorisationInfo,
              2,
              wasSupplementaryUnitAsked = true
            )

            val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
              testEori,
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              testRecordId,
              categoryRecord
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 13
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "categorisation"
            auditDetails("eori") mustBe testEori
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("recordId") mustBe testRecordId
            auditDetails(commodityCodeKey) mustBe "1234567890"
            auditDetails(countryOfOriginKey) mustBe "BV"
            auditDetails("descendants") mustBe "1"
            auditDetails("categoryAssessments") mustBe "3"
            auditDetails("categoryAssessmentsAnswered") mustBe "2"
            auditDetails("reassessmentNeeded") mustBe "false"
            auditDetails("category") mustBe "1"
            auditDetails("providedSupplementaryUnit") mustBe "false"
          }

          "with supplementary unit asked and supplied" in {

            val categoryRecord = CategoryRecord(
              testEori,
              testRecordId,
              "1234567890",
              Category1Scenario,
              Some("Weight, in kilograms"),
              Some("858.321"),
              categorisationInfo,
              2,
              wasSupplementaryUnitAsked = true
            )

            val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
              testEori,
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              testRecordId,
              categoryRecord
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 14
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "categorisation"
            auditDetails("eori") mustBe testEori
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("recordId") mustBe testRecordId
            auditDetails(commodityCodeKey) mustBe "1234567890"
            auditDetails(countryOfOriginKey) mustBe "BV"
            auditDetails("descendants") mustBe "1"
            auditDetails("categoryAssessments") mustBe "3"
            auditDetails("categoryAssessmentsAnswered") mustBe "2"
            auditDetails("reassessmentNeeded") mustBe "false"
            auditDetails("category") mustBe "1"
            auditDetails("providedSupplementaryUnit") mustBe "true"
            auditDetails("supplementaryUnit") mustBe "858.321 Weight, in kilograms"
          }

          "with longer commodity code asked and supplementary unit asked and supplied" in {

            val shorterCode = categorisationInfo.copy(commodityCode = "998877")
            val longerCode  =
              categorisationInfo.copy(
                categoryAssessmentsThatNeedAnswers = Seq(category1, category2, category3, category1),
                descendantCount = 0
              )

            val categoryRecord = CategoryRecord(
              testEori,
              testRecordId,
              "1234567890",
              Category2Scenario,
              Some("Weight, in kilograms"),
              Some("99"),
              shorterCode,
              3,
              wasSupplementaryUnitAsked = true,
              Some(longerCode),
              Some(3),
              Some(2)
            )

            val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
              testEori,
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              testRecordId,
              categoryRecord
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 18
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "categorisation"
            auditDetails("eori") mustBe testEori
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("recordId") mustBe testRecordId
            auditDetails(commodityCodeKey) mustBe "998877"
            auditDetails(countryOfOriginKey) mustBe "BV"
            auditDetails("descendants") mustBe "1"
            auditDetails("categoryAssessments") mustBe "3"
            auditDetails("categoryAssessmentsAnswered") mustBe "3"
            auditDetails("reassessmentNeeded") mustBe "true"
            auditDetails("reassessmentCommodityCode") mustBe "1234567890"
            auditDetails("reassessmentCategoryAssessments") mustBe "4"
            auditDetails("reassessmentCategoryAssessmentsAnswered") mustBe "1"
            auditDetails("reassessmentCategoryAssessmentsCarriedOver") mustBe "2"
            auditDetails("category") mustBe "2"
            auditDetails("providedSupplementaryUnit") mustBe "true"
            auditDetails("supplementaryUnit") mustBe "99 Weight, in kilograms"
          }

          "with longer commodity code asked and supplementary unit asked and supplied but no measurement unit" in {

            val shorterCode = categorisationInfo.copy(commodityCode = "998877")
            val longerCode  =
              categorisationInfo.copy(
                categoryAssessmentsThatNeedAnswers = Seq(category1, category2, category3, category1),
                descendantCount = 0
              )

            val categoryRecord = CategoryRecord(
              testEori,
              testRecordId,
              "1234567890",
              Category2Scenario,
              None,
              Some("99"),
              shorterCode,
              3,
              wasSupplementaryUnitAsked = true,
              Some(longerCode),
              Some(3),
              Some(2)
            )

            val result = AuditEventFactory().createSubmitGoodsRecordEventForCategorisation(
              testEori,
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              testRecordId,
              categoryRecord
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 18
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "categorisation"
            auditDetails("eori") mustBe testEori
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("recordId") mustBe testRecordId
            auditDetails(commodityCodeKey) mustBe "998877"
            auditDetails(countryOfOriginKey) mustBe "BV"
            auditDetails("descendants") mustBe "1"
            auditDetails("categoryAssessments") mustBe "3"
            auditDetails("categoryAssessmentsAnswered") mustBe "3"
            auditDetails("reassessmentNeeded") mustBe "true"
            auditDetails("reassessmentCommodityCode") mustBe "1234567890"
            auditDetails("reassessmentCategoryAssessments") mustBe "4"
            auditDetails("reassessmentCategoryAssessmentsAnswered") mustBe "1"
            auditDetails("reassessmentCategoryAssessmentsCarriedOver") mustBe "2"
            auditDetails("category") mustBe "2"
            auditDetails("providedSupplementaryUnit") mustBe "true"
            auditDetails("supplementaryUnit") mustBe "99 Measurement Unit not found"
          }
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
            UpdateRecordJourney,
            UpdateGoodsRecord(
              testEori,
              testRecordId,
              Some("GB"),
              Some("goods description"),
              Some("trader reference"),
              Some(commodity)
            ),
            testRecordId
          )

          result.auditSource mustBe "trader-goods-profiles-frontend"
          result.auditType mustBe "SubmitGoodsRecord"
          result.tags.isEmpty mustBe false

          val auditDetails = result.detail
          auditDetails.size mustBe 12
          auditDetails("journey") mustBe "UpdateRecord"
          auditDetails("updateSection") mustBe "goodsDetails"
          auditDetails("eori") mustBe testEori
          auditDetails("recordId") mustBe testRecordId
          auditDetails("affinityGroup") mustBe "Organisation"
          auditDetails(traderReferenceKey) mustBe "trader reference"
          auditDetails(goodsDescriptionKey) mustBe "goods description"
          auditDetails(countryOfOriginKey) mustBe "GB"
          auditDetails(commodityCodeKey) mustBe "030821"
          auditDetails("commodityDescription") mustBe "Sea urchins"
          auditDetails("commodityCodeEffectiveFrom") mustBe effectiveFrom.toString
          auditDetails("commodityCodeEffectiveTo") mustBe effectiveTo.toString

        }

        "and update is for supplementary unit" - {

          "when hasSupp is present" in {
            val result = AuditEventFactory().createSubmitGoodsRecordEventForUpdateSupplementaryUnit(
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              SupplementaryRequest(
                testEori,
                testRecordId,
                Some(true),
                Some("supplementaryUnit"),
                Some("measurementUnit")
              ),
              testRecordId
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 7
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "supplementaryUnit"
            auditDetails("eori") mustBe testEori
            auditDetails("recordId") mustBe testRecordId
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("addSupplementaryUnit") mustBe "true"
            auditDetails("supplementaryUnit") mustBe "supplementaryUnit measurementUnit"
          }

          "when hasSupp is not present" in {
            val result = AuditEventFactory().createSubmitGoodsRecordEventForUpdateSupplementaryUnit(
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              SupplementaryRequest(
                testEori,
                testRecordId,
                None,
                Some("supplementaryUnit"),
                Some("measurementUnit")
              ),
              testRecordId
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 7
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "supplementaryUnit"
            auditDetails("eori") mustBe testEori
            auditDetails("recordId") mustBe testRecordId
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("addSupplementaryUnit") mustBe "false"
            auditDetails("supplementaryUnit") mustBe "supplementaryUnit measurementUnit"
          }

          "when suppUnit is not present" in {
            val result = AuditEventFactory().createSubmitGoodsRecordEventForUpdateSupplementaryUnit(
              AffinityGroup.Organisation,
              UpdateRecordJourney,
              SupplementaryRequest(
                testEori,
                testRecordId,
                Some(false),
                None,
                Some("measurementUnit")
              ),
              testRecordId
            )

            result.auditSource mustBe "trader-goods-profiles-frontend"
            result.auditType mustBe "SubmitGoodsRecord"
            result.tags.isEmpty mustBe false

            val auditDetails = result.detail
            auditDetails.size mustBe 6
            auditDetails("journey") mustBe "UpdateRecord"
            auditDetails("updateSection") mustBe "supplementaryUnit"
            auditDetails("eori") mustBe testEori
            auditDetails("recordId") mustBe testRecordId
            auditDetails("affinityGroup") mustBe "Organisation"
            auditDetails("addSupplementaryUnit") mustBe "false"
          }
        }
      }
    }

    "create event when journey is advice request" in {
      val adviceRequest = AdviceRequest(testEori, "Firstname Lastname", "actorId", testRecordId, "test@test.com")

      val result =
        AuditEventFactory().createRequestAdviceEvent(AffinityGroup.Individual, RequestAdviceJourney, adviceRequest)

      result.auditSource mustBe "trader-goods-profiles-frontend"
      result.auditType mustBe "AdviceRequestUpdate"
      result.tags.isEmpty mustBe false

      val auditDetails = result.detail
      auditDetails.size mustBe 6
      auditDetails("journey") mustBe "RequestAdvice"
      auditDetails("affinityGroup") mustBe "Individual"
      auditDetails("eori") mustBe testEori
      auditDetails("recordId") mustBe testRecordId
      auditDetails("requestorName") mustBe "Firstname Lastname"
      auditDetails("requestorEmail") mustBe "test@test.com"
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
            goodsNomenclature =
              GoodsNomenclatureResponse("id", "commodity code", None, Instant.EPOCH, None, List("test", "test1")),
            categoryAssessmentRelationships = Seq(
              CategoryAssessmentRelationship("assessmentId1"),
              CategoryAssessmentRelationship("assessmentId2")
            ),
            includedElements = Seq(
              CategoryAssessmentResponse("assessmentId1", "themeId1", Nil, "regulationId1"),
              ThemeResponse("themeId1", 1, "theme description"),
              CategoryAssessmentResponse(
                "assessmentId2",
                "themeId2",
                Seq(
                  ExemptionResponse("exemptionId1", ExemptionType.Certificate),
                  ExemptionResponse("exemptionId2", ExemptionType.AdditionalCode)
                ),
                "regulationId2"
              ),
              ThemeResponse("themeId2", 2, "theme description"),
              CertificateResponse("exemptionId1", "code1", "description1"),
              AdditionalCodeResponse("exemptionId2", "code2", "description2"),
              ThemeResponse("ignoredTheme", 3, "theme description"),
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

    "create maintain profile event" - {

      "create event on UKIMSNumber change" in {

        val traderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123456"),
            Some("612345"),
            eoriChanged = false
          )

        val updatedTraderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081801",
            Some("RMS-GB-123456"),
            Some("612345"),
            eoriChanged = false
          )

        val result =
          AuditEventFactory().createMaintainProfileEvent(traderProfile, updatedTraderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "MaintainProfile"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 4
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("previousProfile") mustBe Json.stringify(
          Json.obj("UKIMSNumber" -> "XIUKIM47699357400020231115081800")
        )
        auditDetails("currentProfile") mustBe Json.stringify(
          Json.obj("UKIMSNumber" -> "XIUKIM47699357400020231115081801")
        )
      }

      "create event on NIRMSNumber change" in {

        val traderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123456"),
            Some("612345"),
            eoriChanged = false
          )

        val updatedTraderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123457"),
            Some("612345"),
            eoriChanged = false
          )

        val result =
          AuditEventFactory().createMaintainProfileEvent(traderProfile, updatedTraderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "MaintainProfile"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 4
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("previousProfile") mustBe Json.stringify(
          Json.obj("NIRMSRegistered" -> "true", "NIRMSNumber" -> "RMS-GB-123456")
        )
        auditDetails("currentProfile") mustBe Json.stringify(
          Json.obj("NIRMSRegistered" -> "true", "NIRMSNumber" -> "RMS-GB-123457")
        )
      }

      "create event on NIPHLNumber change" in {

        val traderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123456"),
            Some("612345"),
            eoriChanged = false
          )

        val updatedTraderProfile =
          TraderProfile(
            testEori,
            "XIUKIM47699357400020231115081800",
            Some("RMS-GB-123456"),
            Some("612346"),
            eoriChanged = false
          )

        val result =
          AuditEventFactory().createMaintainProfileEvent(traderProfile, updatedTraderProfile, AffinityGroup.Individual)

        result.auditSource mustBe "trader-goods-profiles-frontend"
        result.auditType mustBe "MaintainProfile"
        result.tags.isEmpty mustBe false

        val auditDetails = result.detail
        auditDetails.size mustBe 4
        auditDetails("eori") mustBe testEori
        auditDetails("affinityGroup") mustBe "Individual"
        auditDetails("previousProfile") mustBe Json.stringify(
          Json.obj("NIPHLRegistered" -> "true", "NIPHLNumber" -> "612345")
        )
        auditDetails("currentProfile") mustBe Json.stringify(
          Json.obj("NIPHLRegistered" -> "true", "NIPHLNumber" -> "612346")
        )
      }
    }

    "create event when user clicks on external link" in {
      val result =
        AuditEventFactory().createOutboundClickEvent(AffinityGroup.Individual, testEori, "link", "linkText", "page")

      result.auditSource mustBe "trader-goods-profiles-frontend"
      result.auditType mustBe "OutboundClicks"
      result.tags.isEmpty mustBe false

      val auditDetails = result.detail
      auditDetails.size mustBe 5
      auditDetails("affinityGroup") mustBe "Individual"
      auditDetails("eori") mustBe testEori
      auditDetails("outboundLink") mustBe "link"
      auditDetails("outboundLinkText") mustBe "linkText"
      auditDetails("page") mustBe "page"
    }
  }
}
