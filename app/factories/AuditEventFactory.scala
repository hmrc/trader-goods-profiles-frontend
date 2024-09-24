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

import models.audits._
import models.helper.{CategorisationUpdate, GoodsDetailsUpdate, Journey, UpdateSection}
import models.ott.CategorisationInfo
import models.ott.response.OttResponse
import models.{AdviceRequest, GoodsRecord, TraderProfile, UpdateGoodsRecord}
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import utils.HttpStatusCodeDescriptions.codeDescriptions

import java.time.Instant

case class AuditEventFactory() {

  private val auditSource = "trader-goods-profiles-frontend"

  def createSetUpProfileEvent(
    traderProfile: TraderProfile,
    affinityGroup: AffinityGroup
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori"          -> traderProfile.actorId,
      "affinityGroup" -> affinityGroup.toString,
      "UKIMSNumber"   -> traderProfile.ukimsNumber
    ) ++
      writeOptionalWithAssociatedBooleanFlag("NIRMSRegistered", "NIRMSNumber", traderProfile.nirmsNumber) ++
      writeOptionalWithAssociatedBooleanFlag("NIPHLRegistered", "NIPHLNumber", traderProfile.niphlNumber)

    DataEvent(
      auditSource = auditSource,
      auditType = "ProfileSetUp",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createMaintainProfileEvent(
    traderProfile: TraderProfile,
    updatedTraderProfile: TraderProfile,
    affinityGroup: AffinityGroup
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori"            -> traderProfile.actorId,
      "affinityGroup"   -> affinityGroup.toString,
      "previousProfile" -> addUpdatedFieldsOfTraderProfile(traderProfile, updatedTraderProfile),
      "currentProfile"  -> addUpdatedFieldsOfTraderProfile(updatedTraderProfile, traderProfile)
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "MaintainProfile",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createStartManageGoodsRecordEvent(
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    updateSection: Option[UpdateSection],
    recordId: Option[String],
    commodity: Option[CategorisationInfo] = None,
  )(implicit hc: HeaderCarrier): DataEvent = {

    val auditDetails = Map(
      "journey"       -> journey.toString,
      "eori"          -> eori,
      "affinityGroup" -> affinityGroup.toString
    ) ++
      writeOptional("updateSection", updateSection.map(_.toString)) ++
      writeOptional("recordId", recordId) ++
      writeOptional("commodityCode", commodity.map(_.commodityCode)) ++
      writeOptional("descendants", commodity.map(_.descendantCount.toString))

    DataEvent(
      auditSource = auditSource,
      auditType = "StartManageGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  }

  def createSubmitGoodsRecordEventForCreateRecord(
    affinityGroup: AffinityGroup,
    journey: Journey,
    goodsRecord: GoodsRecord,
    isUsingGoodsDescription: Boolean
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori"                       -> goodsRecord.eori,
      "affinityGroup"              -> affinityGroup.toString,
      "journey"                    -> journey.toString,
      "traderReference"            -> goodsRecord.traderRef,
      "goodsDescription"           -> goodsRecord.goodsDescription,
      "specifiedGoodsDescription"  -> isUsingGoodsDescription.toString,
      "countryOfOrigin"            -> goodsRecord.countryOfOrigin,
      "commodityCode"              -> goodsRecord.commodity.commodityCode,
      "commodityDescription"       -> goodsRecord.commodity.descriptions.headOption.getOrElse("null"),
      "commodityCodeEffectiveFrom" -> goodsRecord.commodity.validityStartDate.toString,
      "commodityCodeEffectiveTo"   -> goodsRecord.commodity.validityEndDate.map(_.toString).getOrElse("null")
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createSubmitGoodsRecordEventForCategorisation(
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    recordId: String,
    categoryAssessmentsWithExemptions: Int,
    category: Int,
//    categorisationInfo: CategorisationInfo
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "journey" -> journey.toString,
      "updateSection" -> CategorisationUpdate.toString,
      "recordId" -> recordId,
      "eori"                              -> eori,
      "affinityGroup"                     -> affinityGroup.toString,
//      "commodityCode" -> categorisationInfo.commodityCode,
//      "descendants" -> categorisationInfo.descendantCount,
      "categoryAssessmentsWithExemptions" -> categoryAssessmentsWithExemptions.toString,
      "category"                          -> category.toString
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createSubmitGoodsRecordEventForUpdateRecord(
    affinityGroup: AffinityGroup,
    journey: Journey,
    goodsRecord: UpdateGoodsRecord,
    recordId: String
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "journey"       -> journey.toString,
      "updateSection" -> GoodsDetailsUpdate.toString,
      "recordId"      -> recordId,
      "eori"          -> goodsRecord.eori,
      "affinityGroup" -> affinityGroup.toString
    ) ++
      writeOptional("commodityCode", goodsRecord.commodityCode.map(_.commodityCode)) ++
      writeOptional("commodityDescription", goodsRecord.commodityCode.flatMap(_.descriptions.headOption)) ++
      writeOptional("commodityCodeEffectiveFrom", goodsRecord.commodityCode.map(_.validityStartDate.toString)) ++
      writeOptional(
        "commodityCodeEffectiveTo",
        goodsRecord.commodityCode.map(_.validityEndDate.map(_.toString).getOrElse("null"))
      ) ++
      writeOptional("goodsDescription", goodsRecord.goodsDescription) ++
      writeOptional("traderReference", goodsRecord.traderReference) ++
      writeOptional("countryOfOrigin", goodsRecord.countryOfOrigin)

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createSubmitGoodsRecordEventForRemoveRecord(
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    recordId: String
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori"          -> eori,
      "affinityGroup" -> affinityGroup.toString,
      "journey"       -> journey.toString,
      "recordId"      -> recordId
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createRequestAdviceEvent(
    affinityGroup: AffinityGroup,
    journey: Journey,
    adviceRequest: AdviceRequest
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "journey"        -> journey.toString,
      "eori"           -> adviceRequest.eori,
      "affinityGroup"  -> affinityGroup.toString,
      "recordId"       -> adviceRequest.recordId,
      "requestorName"  -> adviceRequest.requestorName,
      "requestorEmail" -> adviceRequest.requestorEmail
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "AdviceRequestUpdate",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createWithdrawAdviceEvent(
    affinityGroup: AffinityGroup,
    eori: String,
    journey: Journey,
    recordId: String,
    withdrawReason: String
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "journey"        -> journey.toString,
      "eori"           -> eori,
      "affinityGroup"  -> affinityGroup.toString,
      "recordId"       -> recordId,
      "withdrawReason" -> withdrawReason
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "AdviceRequestUpdate",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createValidateCommodityCodeEvent(
    auditData: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: Option[String],
    commodityDetails: Option[OttResponse]
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val auditDetails = ValidateCommodityCodeEvent(
      auditData.eori,
      auditData.affinityGroup.toString,
      auditData.journey.map(_.toString),
      auditData.recordId,
      auditData.commodityCode,
      requestDateTime.toString,
      responseDateTime.toString,
      ValidateCommodityCodeEventOutcome(
        if (responseStatus == OK) "valid" else "invalid",
        codeDescriptions(responseStatus),
        responseStatus.toString,
        errorMessage
      ),
      commodityDetails.flatMap(_.goodsNomenclature.descriptions.headOption),
      // If commodityDetails are defined and no endDate then we got sent a null for this so pass it on.
      commodityDetails.map(_.goodsNomenclature.validityEndDate.map(_.toString).getOrElse("null")),
      commodityDetails.map(_.goodsNomenclature.validityStartDate.toString)
    )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = "ValidateCommodityCode",
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
  }

  def createGetCategorisationAssessmentDetailsEvent(
    auditData: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: Option[String],
    ottResponse: Option[OttResponse]
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val auditDetails = GetCategorisationAssessmentDetailsEvent(
      auditData.eori,
      auditData.affinityGroup.toString,
      auditData.recordId,
      auditData.commodityCode,
      auditData.countryOfOrigin,
      auditData.dateOfTrade.map(_.toString),
      requestDateTime.toString,
      responseDateTime.toString,
      GetCategorisationAssessmentDetailsEventOutcome(
        codeDescriptions(responseStatus),
        responseStatus.toString,
        errorMessage
      ),
      ottResponse.map(_.categoryAssessments.size.toString),
      ottResponse.map(_.categoryAssessments.map(_.exemptions.size).sum.toString)
    )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = "GetCategorisationAssessmentDetails",
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
  }

  private def createSubmitGoodsRecordEvent(auditDetails: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = auditSource,
      auditType = "SubmitGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  private def addUpdatedFieldsOfTraderProfile(
    traderProfile: TraderProfile,
    updatedTraderProfile: TraderProfile
  ): String =
    if (traderProfile.ukimsNumber != updatedTraderProfile.ukimsNumber) {
      Json.stringify(Json.obj("UKIMSNumber" -> traderProfile.ukimsNumber))
    } else if (traderProfile.nirmsNumber != updatedTraderProfile.nirmsNumber) {
      Json.stringify(
        Json.toJson(writeOptionalWithAssociatedBooleanFlag("NIRMSRegistered", "NIRMSNumber", traderProfile.nirmsNumber))
      )
    } else {
      Json.stringify(
        Json.toJson(writeOptionalWithAssociatedBooleanFlag("NIPHLRegistered", "NIPHLNumber", traderProfile.niphlNumber))
      )
    }

  private def writeOptionalWithAssociatedBooleanFlag(
    booleanFlagDescription: String,
    valueDescription: String,
    optionalValue: Option[String]
  )                                                                                  =
    optionalValue
      .map { value =>
        Map(booleanFlagDescription -> "true", valueDescription -> value)
      }
      .getOrElse(Map(booleanFlagDescription -> "false"))

  private def writeOptional(valueDescription: String, optionalValue: Option[String]) =
    optionalValue
      .map { value =>
        Map(valueDescription -> value)
      }
      .getOrElse(Map.empty)
}
