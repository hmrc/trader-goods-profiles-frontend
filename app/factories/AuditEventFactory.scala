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
import models.ott.response.OttResponse
import models.{Commodity, GoodsRecord, TraderProfile}
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

  def createStartManageGoodsRecordEvent(
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    updateSection: Option[UpdateSection],
    recordId: Option[String]
  )(implicit hc: HeaderCarrier): DataEvent = {

    val auditDetails = Map(
      "journey" -> journey.toString,
      "eori" -> eori,
      "affinityGroup" -> affinityGroup.toString,
    ) ++
      writeOptional("updateSection", updateSection.map(_.toString)) ++
      writeOptional("recordId", recordId)

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
      "eori" -> goodsRecord.eori,
      "affinityGroup" -> affinityGroup.toString,
      "journey" -> journey.toString,
      "traderReference" -> goodsRecord.traderRef,
      "goodsDescription" -> goodsRecord.goodsDescription,
      "specifiedGoodsDescription" -> isUsingGoodsDescription.toString,
      "countryOfOrigin" -> goodsRecord.countryOfOrigin,
      "commodityCode" -> goodsRecord.commodity.commodityCode,
      "commodityDescription" -> goodsRecord.commodity.description,
      "commodityCodeEffectiveFrom" -> goodsRecord.commodity.validityStartDate.toString,
      "commodityCodeEffectiveTo" -> goodsRecord.commodity.validityEndDate.map(_.toString).getOrElse("null")
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createSubmitGoodsRecordEventForCategorisation(
    eori: String,
    affinityGroup: AffinityGroup,
    journey: Journey,
    recordId: String,
    categoryAssessmentsWithExemptions: Int,
    category: Int //TODO not an int use type
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori" -> eori,
      "affinityGroup" -> affinityGroup.toString,
      "journey" -> journey.toString,
      "updateSection" -> CategorisationUpdate.toString,
      "recordId" -> recordId,
      "categoryAssessmentsWithExemptions" -> categoryAssessmentsWithExemptions.toString,
      "category" -> category.toString
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }

  def createSubmitGoodsRecordEventForUpdateRecord(
    affinityGroup: AffinityGroup,
    journey: Journey,
    goodsRecord: GoodsRecord
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "eori" -> goodsRecord.eori,
      "affinityGroup" -> affinityGroup.toString,
      "journey" -> journey.toString,
      "updateSection" -> GoodsDetailsUpdate.toString,
      "traderReference" -> goodsRecord.traderRef,
      "goodsDescription" -> goodsRecord.goodsDescription,
      "countryOfOrigin" -> goodsRecord.countryOfOrigin,
      "commodityCode" -> goodsRecord.commodity.commodityCode,
      "commodityDescription" -> goodsRecord.commodity.description,
      "commodityCodeEffectiveFrom" -> goodsRecord.commodity.validityStartDate.toString,
      "commodityCodeEffectiveTo" -> goodsRecord.commodity.validityEndDate.map(_.toString).getOrElse("null")
    )

    createSubmitGoodsRecordEvent(auditDetails)
  }


  private def createSubmitGoodsRecordEvent(auditDetails: Map[String, String])(implicit hc: HeaderCarrier) = {
    DataEvent(
      auditSource = auditSource,
      auditType = "SubmitGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createStartCreateGoodsRecord(
    eori: String,
    affinityGroup: AffinityGroup
  )(implicit hc: HeaderCarrier): DataEvent = {

    val auditDetails = Map(
      "EORINumber"    -> eori,
      "affinityGroup" -> affinityGroup.toString
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "StartCreateGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  }

  //TODO remove
  def createStartUpdateGoodsRecord(
    eori: String,
    affinityGroup: AffinityGroup,
    updateSection: String,
    recordId: String
  )(implicit hc: HeaderCarrier): DataEvent = {

    val auditDetails = Map(
      "EORINumber"    -> eori,
      "affinityGroup" -> affinityGroup.toString,
      "updateSection" -> updateSection,
      "recordId"      -> recordId
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "StartUpdateGoodsRecord",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )

  }

  //TODO remove
  def createFinishCreateGoodsRecord(
    affinityGroup: AffinityGroup,
    goodsRecord: GoodsRecord,
    commodity: Commodity,
    isUsingGoodsDescription: Boolean
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "EORINumber"                 -> goodsRecord.eori,
      "affinityGroup"              -> affinityGroup.toString,
      "traderReference"            -> goodsRecord.traderRef,
      "commodityCode"              -> goodsRecord.commodity.commodityCode,
      "countryOfOrigin"            -> goodsRecord.countryOfOrigin,
      "commodityDescription"       -> commodity.description,
      "commodityCodeEffectiveFrom" -> commodity.validityStartDate.toString,
      "commodityCodeEffectiveTo"   -> commodity.validityEndDate.map(_.toString).getOrElse("null"),
      "specifiedGoodsDescription"  -> isUsingGoodsDescription.toString,
      "goodsDescription"           -> goodsRecord.goodsDescription
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "FinishCreateGoodsRecord",
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
    commodityDetails: Option[Commodity]
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val auditDetails = ValidateCommodityCodeEvent(
      auditData.eori,
      auditData.affinityGroup.toString,
      auditData.journey.map(_.toString).getOrElse("null"),
      auditData.recordId.getOrElse("null"),
      auditData.commodityCode,
      requestDateTime.toString,
      responseDateTime.toString,
      ValidateCommodityCodeEventOutcome(
        if (responseStatus == OK) "valid" else "invalid",
        codeDescriptions(responseStatus),
        responseStatus.toString,
        errorMessage.getOrElse("null")
      ),
      commodityDetails.map(_.description).getOrElse("null"),
      commodityDetails.flatMap(_.validityEndDate.map(_.toString)).getOrElse("null"),
      commodityDetails.map(_.validityStartDate.toString).getOrElse("null")
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
      auditData.recordId.getOrElse("null"),
      auditData.commodityCode,
      auditData.countryOfOrigin.getOrElse("null"),
      auditData.dateOfTrade.map(_.toString).getOrElse("null"),
      requestDateTime.toString,
      responseDateTime.toString,
      GetCategorisationAssessmentDetailsEventOutcome(
        codeDescriptions(responseStatus),
        responseStatus.toString,
        errorMessage.getOrElse("null")
      ),
      ottResponse.map(_.categoryAssessments.size.toString).getOrElse("null"),
      ottResponse.map(_.categoryAssessments.map(_.exemptions.size).sum.toString).getOrElse("null")
    )

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = "GetCategorisationAssessmentDetails",
      tags = hc.toAuditTags(),
      detail = Json.toJson(auditDetails)
    )
  }

  private def writeOptionalWithAssociatedBooleanFlag(booleanFlagDescription: String, valueDescription: String, optionalValue: Option[String]) =
    optionalValue
      .map { value =>
        Map(booleanFlagDescription -> "true", valueDescription -> value)
      }
      .getOrElse(Map(booleanFlagDescription -> "false"))

  private def writeOptional(valueDescription: String, optionalValue: Option[String]) =
    optionalValue
      .map { value =>
        Map(valueDescription -> value)
      }.getOrElse(Map.empty)
}
