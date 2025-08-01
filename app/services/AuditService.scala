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

import com.google.inject.Inject
import factories.AuditEventFactory
import models.audits._
import models.helper._
import models.ott.CategorisationInfo
import models.ott.response.OttResponse
import models._
import org.apache.pekko.Done
import play.api.Logging
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits.catsSyntaxTuple5Parallel

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

  def auditMaintainProfile(
    traderProfile: TraderProfile,
    updatedTraderProfile: TraderProfile,
    affinityGroup: AffinityGroup
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createMaintainProfileEvent(traderProfile, updatedTraderProfile, affinityGroup)
    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"MaintainProfile audit event status: $auditResult")
      Done
    }
  }

  def auditStartCreateGoodsRecord(eori: String, affinityGroup: AffinityGroup)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event =
      auditEventFactory.createStartManageGoodsRecordEvent(
        eori,
        affinityGroup,
        CreateRecordJourney,
        None,
        None,
        None
      )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"StartCreateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditStartRemoveGoodsRecord(eori: String, affinityGroup: AffinityGroup, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event =
      auditEventFactory.createStartManageGoodsRecordEvent(eori, affinityGroup, RemoveRecord, None, Some(recordId))

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"auditStartRemoveGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditFinishRemoveGoodsRecord(eori: String, affinityGroup: AffinityGroup, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event =
      auditEventFactory.createSubmitGoodsRecordEventForRemoveRecord(eori, affinityGroup, RemoveRecord, recordId)

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"FinishRemoveGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditFinishCreateGoodsRecord(
    eori: String,
    affinityGroup: AffinityGroup,
    userAnswers: UserAnswers,
    categoryRecordOpt: Option[CategoryRecord],
    isAutoCategorised: Boolean
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val buildEvent = (
      Right(affinityGroup),
      Right(CreateRecordJourney),
      GoodsRecord.build(userAnswers, eori),
      Right(categoryRecordOpt),
      Right(isAutoCategorised)
    ).parMapN(auditEventFactory.createSubmitGoodsRecordEventForCreateRecord)

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

  def auditFinishUpdateGoodsRecord(
    recordId: String,
    affinityGroup: AffinityGroup,
    updateGoodsRecord: UpdateGoodsRecord
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val event = auditEventFactory.createSubmitGoodsRecordEventForUpdateRecord(
      affinityGroup,
      UpdateRecordJourney,
      updateGoodsRecord,
      recordId
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"FinishUpdateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditFinishUpdateSupplementaryUnitGoodsRecord(
    recordId: String,
    affinityGroup: AffinityGroup,
    supplementaryRequest: SupplementaryRequest
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    val event = auditEventFactory.createSubmitGoodsRecordEventForUpdateSupplementaryUnit(
      affinityGroup,
      UpdateRecordJourney,
      supplementaryRequest,
      recordId
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"FinishUpdateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditRequestAdvice(
    affinityGroup: AffinityGroup,
    adviceRequest: AdviceRequest
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createRequestAdviceEvent(affinityGroup, RequestAdviceJourney, adviceRequest)

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"RequestAdvice audit event status: $auditResult")
      Done
    }
  }

  def auditFilterSearchRecords(
    affinityGroup: AffinityGroup,
    SearchForm: SearchForm,
    totalRecords: String,
    totalPages: String,
    eori: String
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event =
      auditEventFactory.createSearchFilterRecordEvent(affinityGroup, SearchForm, totalRecords, totalPages, eori)
    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"SearchGoodsRecords audit event status: $auditResult")
      Done
    }

  }

  def auditWithdrawAdvice(affinityGroup: AffinityGroup, eori: String, recordId: String, withdrawReason: Option[String])(
    implicit hc: HeaderCarrier
  ): Future[Done] = {
    val event =
      auditEventFactory.createWithdrawAdviceEvent(
        affinityGroup,
        eori,
        WithdrawAdviceJourney,
        recordId,
        withdrawReason.getOrElse("")
      )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"WithdrawAdvice audit event status: $auditResult")
      Done
    }
  }

  def auditFinishCategorisation(
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: String,
    categoryRecord: CategoryRecord
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createSubmitGoodsRecordEventForCategorisation(
      eori,
      affinityGroup,
      UpdateRecordJourney,
      recordId,
      categoryRecord
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"SubmitGoodsRecordEvent audit event status: $auditResult")
      Done
    }

  }

  def auditStartUpdateGoodsRecord(
    eori: String,
    affinityGroup: AffinityGroup,
    updateSection: UpdateSection,
    recordId: String,
    categoryDetails: Option[CategorisationInfo] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createStartManageGoodsRecordEvent(
      eori,
      affinityGroup,
      UpdateRecordJourney,
      Some(updateSection),
      Some(recordId),
      categoryDetails
    )

    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"StartUpdateGoodsRecord audit event status: $auditResult")
      Done
    }
  }

  def auditOttCall[T](
    auditDetails: Option[OttAuditData],
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: Option[String],
    response: Option[T]
  )(implicit hc: HeaderCarrier): Future[Done] =
    auditDetails match {
      case Some(details) =>
        details.auditMode match {
          case AuditValidateCommodityCode =>
            auditValidateCommodityCode(
              details,
              requestDateTime,
              responseDateTime,
              responseStatus,
              errorMessage,
              response.map(x => x.asInstanceOf[OttResponse])
            )

          case AuditGetCategorisationAssessment =>
            auditGetCategorisationAssessmentDetails(
              details,
              requestDateTime,
              responseDateTime,
              responseStatus,
              errorMessage,
              response.map(x => x.asInstanceOf[OttResponse])
            )
        }

      case _ => Future.successful(Done)
    }

  private def auditValidateCommodityCode(
    auditDetails: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: Option[String],
    commodityDetails: Option[OttResponse]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createValidateCommodityCodeEvent(
      auditDetails,
      requestDateTime,
      responseDateTime,
      responseStatus,
      errorMessage,
      commodityDetails
    )

    auditConnector.sendExtendedEvent(event).map { auditResult =>
      logger.info(s"ValidateCommodityCode audit event status: $auditResult")
      Done
    }
  }

  private def auditGetCategorisationAssessmentDetails(
    auditDetails: OttAuditData,
    requestDateTime: Instant,
    responseDateTime: Instant,
    responseStatus: Int,
    errorMessage: Option[String],
    ottResponse: Option[OttResponse]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val event = auditEventFactory.createGetCategorisationAssessmentDetailsEvent(
      auditDetails,
      requestDateTime,
      responseDateTime,
      responseStatus,
      errorMessage,
      ottResponse
    )

    auditConnector.sendExtendedEvent(event).map { auditResult =>
      logger.info(s"GetCategorisationAssessmentDetails audit event status: $auditResult")
      Done
    }

  }

  def auditOutboundClick(
    affinityGroup: AffinityGroup,
    eori: String,
    link: String,
    linkText: String,
    page: String
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] = {
    val event = auditEventFactory.createOutboundClickEvent(affinityGroup, eori, link, linkText, page)
    auditConnector.sendEvent(event).map { auditResult =>
      logger.info(s"OutboundClicks audit event status: $auditResult")
      Done
    }
  }

}
