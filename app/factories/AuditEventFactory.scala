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

import models.audits.OttAuditData
import models.ott.response.OttResponse
import models.{Commodity, GoodsRecord, TraderProfile}
import play.api.http.Status.OK
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.HttpStatusCodeDescriptions.codeDescriptions

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
    auditData: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: String,
    commodityDetails: Option[Commodity]
  )(implicit hc: HeaderCarrier): DataEvent = {

    //TODO will this look right
    val auditDetails = Map(
      "eori"                        -> auditData.eori,
      "affinityGroup"               -> auditData.affinityGroup.toString,
      "journey"                     -> auditData.journey.getOrElse("null"),
      "recordId"                    -> auditData.recordId.getOrElse("null"),
      "commodityCode"               -> auditData.commodityCode,
      "requestDateTime"             -> requestDateTime.toString,
      "responseDateTime"            -> responseDateTime.toString,
      "outcome.commodityCodeStatus" -> (if (responseStatus == OK) "valid" else "invalid"), //TODO test both
      "outcome.status"              -> codeDescriptions(responseStatus),
      "outcome.statusCode"          -> responseStatus.toString,
      "outcome.failureReason"       -> (if (responseStatus == OK) "null" else errorMessage),
      "commodityDescription"        -> commodityDetails.map(_.description).getOrElse("null"),
      "commodityCodeEffectiveTo"    -> commodityDetails.map(_.validityEndDate.toString).getOrElse("null"), //TODO test both
      "commodityCodeEffectiveFrom"  -> commodityDetails.map(_.validityStartDate.toString).getOrElse("null")
    )

    DataEvent(
      auditSource = auditSource,
      auditType = "ValidateCommodityCode",
      tags = hc.toAuditTags(),
      detail = auditDetails
    )
  }

  def createGetCategorisationAssessmentDetailsEvent(
    auditData: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: String,
    ottResponse: Option[OttResponse]
  )(implicit hc: HeaderCarrier): DataEvent = {

    //TODO will this look right
    val auditDetails = Map(
      "eori"                      -> auditData.eori,
      "affinityGroup"             -> auditData.affinityGroup.toString,
      "recordId"                  -> auditData.recordId.getOrElse("null"),
      "commodityCode"             -> auditData.commodityCode,
      "countryOfOrigin"           -> auditData.countryOfOrigin.getOrElse("null"),
      "dateOfTrade"               -> auditData.dateOfTrade.map(_.toString).getOrElse("null"),
      "requestDateTime"           -> requestDateTime.toString,
      "responseDateTime"          -> responseDateTime.toString,
      "outcome.status"            -> codeDescriptions(responseStatus),
      "outcome.statusCode"        -> responseStatus.toString,
      "outcome.failureReason"     -> (if (responseStatus == OK) "null" else errorMessage),
      "categoryAssessmentOptions" -> ottResponse.map(_.categoryAssessments.size.toString).getOrElse("null"),
      "exemptionOptions"          -> ottResponse.map(_.categoryAssessments.map(_.exemptions.size).sum.toString).getOrElse("null")
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
