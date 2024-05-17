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

import base.SpecBase
import base.TestConstants.testEori
import factories.AuditEventFactory
import models.TraderProfile
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.play.audit.model.DataEvent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import org.apache.pekko.Done
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAuditConnector              = mock[AuditConnector]
  private val mockAuditFactory                = mock[AuditEventFactory]
  val auditService                            = new AuditService(mockAuditConnector, mockAuditFactory)
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val startTime = Some(Instant.now())

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuditConnector)
    reset(mockAuditFactory)
  }

  "auditProfileSetUp" - {

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createSetUpProfileEvent(any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val traderProfile = TraderProfile(testEori, "", None, None)
      val result        = await(auditService.auditProfileSetUp(traderProfile, startTime, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the trader profile and time to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createSetUpProfileEvent(eqTo(traderProfile), eqTo(startTime), eqTo(AffinityGroup.Individual))(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createSetUpProfileEvent(any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val traderProfile = TraderProfile(testEori, "", None, None)
      val result        = await(auditService.auditProfileSetUp(traderProfile, startTime, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the trader profile to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createSetUpProfileEvent(eqTo(traderProfile), eqTo(startTime), eqTo(AffinityGroup.Individual))(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {
      when(mockAuditConnector.sendEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        val traderProfile = TraderProfile(testEori, "", None, None)
        await(auditService.auditProfileSetUp(traderProfile, Some(Instant.now()), AffinityGroup.Individual))
      }

    }

  }

}
