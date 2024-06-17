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
import models.{GoodsRecord, TraderProfile, UserAnswers}
import org.apache.pekko.Done
import pages.UseTraderReferencePage
import play.api.Logging
import queries.CommodityQuery
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.{Instant, LocalDate}
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
    eori: String,
    affinityGroup: AffinityGroup,
    journey: String,
    recordId: String,
    commodityCode: String,
    requestDateTime: Instant,
    responseDateTime: Instant,
    commodityCodeStatus: Boolean,
    statusString: String,
    statusCode: Int,
    failureReason: String,
    commodityCodeDescription: String,
    commodityCodeEffectiveTo: Option[Instant],
    commodityCodeEffectiveFrom: Instant
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createValidateCommodityCodeEvent(
      eori, affinityGroup, journey, recordId, commodityCode, requestDateTime, responseDateTime, commodityCodeStatus, statusString, statusCode, failureReason, commodityCodeDescription, commodityCodeEffectiveTo, commodityCodeEffectiveFrom
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"ValidateCommodityCode audit event status: $auditResult")
      Done
    }
  }

    def auditGetCategorisationAssessmentDetails(
      eori: String,
      affinityGroup: AffinityGroup,
      recordId: String,
      commodityCode: String,
      countryOfOrigin: String,
      dateOfTrade: LocalDate,
      requestDateTime: Instant,
      responseDateTime: Instant,
      statusString: String,
      statusCode: Int,
      failureReason: String,
      categoryAssessmentOptions: Int,
      exemptionOptions: Int
    )(implicit hc: HeaderCarrier): Future[Done] = {

      val event = auditEventFactory.createGetCategorisationAssessmentDetailsEvent(eori, affinityGroup, recordId, commodityCode, countryOfOrigin, dateOfTrade, requestDateTime, responseDateTime, statusString, statusCode, failureReason, categoryAssessmentOptions, exemptionOptions)

      auditConnector.sendEvent(event).map { auditResult =>
        logger.info(s"GetCategorisationAssessmentDetails audit event status: $auditResult")
        Done
      }


    }

}
