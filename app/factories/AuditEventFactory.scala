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

import models.{Commodity, GoodsRecord, TraderProfile}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent

import java.time.{Instant, LocalDate}

case class AuditEventFactory() {

  private val auditSource = "trader-goods-profiles-frontend"

  def createSetUpProfileEvent(
    traderProfile: TraderProfile,
    affinityGroup: AffinityGroup
  )(implicit hc: HeaderCarrier): DataEvent = {
    val auditDetails = Map(
      "EORINumber"    -> traderProfile.actorId,
      "affinityGroup" -> affinityGroup.toString,
      "UKIMSNumber"   -> traderProfile.ukimsNumber
    ) ++
      writeOptional("isNIRMSRegistered", "NIRMSNumber", traderProfile.nirmsNumber) ++
      writeOptional("isNIPHLRegistered", "NIPHLNumber", traderProfile.niphlNumber)

    DataEvent(
      auditSource = auditSource,
      auditType = "ProfileSetUp",
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
      "commodityCode"              -> goodsRecord.comcode,
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
    eori: String,
    affinityGroup: AffinityGroup,
    journey: String,
    recordId: Option[String],
    commodityCode: String,
    requestDateTime: Instant,
    responseDateTime: Instant,
    commodityCodeStatus: Boolean,
    statusString: String,
    statusCode: Int,
    failureReason: String,
    commodityCodeDescription: Option[String],
    commodityCodeEffectiveTo: Option[Instant],
    commodityCodeEffectiveFrom: Option[Instant]
  )(implicit hc: HeaderCarrier): DataEvent = {

    //TODO will this look right
    //TODO cleanup parameters
    val auditDetails = Map(
      "eori" -> eori,
      "affinityGroup" -> affinityGroup.toString,
      "journey" -> journey,
      "recordId" -> recordId.getOrElse("null"),
      "commodityCode" -> commodityCode,
      "requestDateTime" -> requestDateTime.toString,
      "responseDateTime" -> responseDateTime.toString,
      "outcome.commodityCodeStatus" -> (if (commodityCodeStatus) "valid" else "invalid"), //TODO test both
      "outcome.status" -> statusString,
      "outcome.statusCode" -> statusCode.toString,
      "outcome.failureReason" -> failureReason,
      "commodityDescription" -> commodityCodeDescription.getOrElse("null"),
      "commodityCodeEffectiveTo" -> commodityCodeEffectiveTo.map(_.toString).getOrElse("null"), //TODO test both
      "commodityCodeEffectiveFrom" -> commodityCodeEffectiveFrom.map(_.toString).getOrElse("null")
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "ValidateCommodityCode",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createGetCategorisationAssessmentDetailsEvent(
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: Option[String],
    commodityCode: String,
    countryOfOrigin: String,
    dateOfTrade: LocalDate,
    requestDateTime: Instant,
    responseDateTime: Instant,
    statusString: String,
    statusCode: Int,
    failureReason: String,
    categoryAssessmentOptions: Option[Int],
    exemptionOptions: Option[Int]
  )(implicit hc: HeaderCarrier): DataEvent = {

    //TODO will this look right
    //TODO cleanup parameters
    val auditDetails = Map(
      "eori" -> eori,
      "affinityGroup" -> affinityGroup.toString,
      "recordId" -> recordId.getOrElse("null"),
      "commodityCode" -> commodityCode,
      "countryOfOrigin" -> countryOfOrigin,
      "dateOfTrade" -> dateOfTrade.toString,
      "requestDateTime" -> requestDateTime.toString,
      "responseDateTime" -> responseDateTime.toString,
      "outcome.status" -> statusString,
      "outcome.statusCode" -> statusCode.toString,
      "outcome.failureReason" -> failureReason,
      "categoryAssessmentOptions" -> categoryAssessmentOptions.map(_.toString).getOrElse("null"),
      "exemptionOptions" -> exemptionOptions.map(_.toString).getOrElse("null"),
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "GetCategorisationAssessmentDetails",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  private def writeOptional(containsValueDescription: String, valueDescription: String, optionalValue: Option[String]) =
    optionalValue
      .map { value =>
        Map(containsValueDescription -> "true", valueDescription -> value)
      }
      .getOrElse(Map(containsValueDescription -> "false"))

}
