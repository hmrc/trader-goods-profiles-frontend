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

package services

import cats.implicits.catsSyntaxTuple4Parallel
import com.google.inject.Inject
import factories.AuditEventFactory
import models.audits.OttAuditDetails
import models.ott.response.OttResponse
import models.{Commodity, GoodsRecord, TraderProfile, UserAnswers}
import org.apache.pekko.Done
import pages.UseTraderReferencePage
import play.api.Logging
import play.api.http.Status.OK
import queries.CommodityQuery
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.HttpStatusCodeDescriptions.codeDescriptions

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (auditConnector: AuditConnector, auditEventFactory: AuditEventFactory)(implicit
  ec: ExecutionContext
) extends Logging {

  def auditProfileSetUp(traderProfile: TraderProfile, affinityGroup: AffinityGroup)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createSetUpProfileEvent(traderProfile, affinityGroup)
    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"SetUpProfile audit event status: $auditResult")
      Done
    }
  }

  def auditStartCreateGoodsRecord(eori: String, affinityGroup: AffinityGroup)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createStartCreateGoodsRecord(eori, affinityGroup)

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"StartCreateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditFinishCreateGoodsRecord(eori: String, affinityGroup: AffinityGroup, userAnswers: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val buildEvent = (
      Right(affinityGroup),
      GoodsRecord.build(userAnswers, eori),
      userAnswers.getPageValue(CommodityQuery),
      userAnswers.getPageValue(UseTraderReferencePage).map(!_)
    ).parMapN(auditEventFactory.createFinishCreateGoodsRecord)

    buildEvent match {
      case Right(event) =>
        auditConnector.sendEvent(event).map { auditResult =>
          logger.info(s"FinishCreateGoodsRecord audit event status: $auditResult")
          Done
        }

      case Left(errors) =>
        logger.info(s"Failed to create FinishCreateGoodsRecord audit event: $errors")
        Future.successful(Done)
    }

  }

  def auditStartUpdateGoodsRecord(eori: String, affinityGroup: AffinityGroup, updateSection: String, recordId: String)(
    implicit hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createStartUpdateGoodsRecord(eori, affinityGroup, updateSection, recordId)

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"StartUpdateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditValidateCommodityCode(
    auditDetails: OttAuditDetails,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: String,
    commodityDetails: Option[Commodity]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createValidateCommodityCodeEvent(
      auditDetails.eori,
      auditDetails.affinityGroup,
      auditDetails.journey,
      auditDetails.recordId,
      auditDetails.commodityCode,
      requestDateTime,
      responseDateTime,
      responseStatus == OK,
      codeDescriptions(responseStatus),
      responseStatus,
      if (responseStatus == OK) {
        "null"
      } else {
        errorMessage
      },
      commodityDetails.map(_.description),
      commodityDetails.flatMap(_.validityEndDate),
      commodityDetails.map(_.validityStartDate)
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"ValidateCommodityCode audit event status: $auditResult")
      Done
    }
  }

  def auditGetCategorisationAssessmentDetails(
    auditDetails: OttAuditDetails,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: String,
    ottResponse: Option[OttResponse]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createGetCategorisationAssessmentDetailsEvent(
      auditDetails.eori,
      auditDetails.affinityGroup,
      auditDetails.recordId,
      auditDetails.commodityCode,
      auditDetails.countryOfOrigin,
      auditDetails.dateOfTrade,
      requestDateTime,
      responseDateTime,
      codeDescriptions(responseStatus),
      responseStatus,
      if (responseStatus == OK) {
        "null"
      } else {
        errorMessage
      },
      ottResponse.map(_.categoryAssessments.size),
      ottResponse.map(_.categoryAssessments.map(_.exemptions.size).sum)
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"GetCategorisationAssessmentDetails audit event status: $auditResult")
      Done
    }

  }

}
