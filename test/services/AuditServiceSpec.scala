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
import base.TestConstants.{testEori, testRecordId}
import factories.AuditEventFactory
import models.audits.OttAuditData
import models.ott.response.{CategoryAssessmentRelationship, GoodsNomenclatureResponse, IncludedElement, OttResponse}
import models.{GoodsRecord, TraderProfile}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import queries.CommodityQuery
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockAuditConnector              = mock[AuditConnector]
  private val mockAuditFactory                = mock[AuditEventFactory]
  val auditService                            = new AuditService(mockAuditConnector, mockAuditFactory)
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuditConnector)
    reset(mockAuditFactory)
  }

  "auditProfileSetUp" - {

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createSetUpProfileEvent(any(), any())(any())).thenReturn(fakeAuditEvent)

      val traderProfile = TraderProfile(testEori, "", None, None)
      val result        = await(auditService.auditProfileSetUp(traderProfile, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the trader profile and affinity group to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createSetUpProfileEvent(eqTo(traderProfile), eqTo(AffinityGroup.Individual))(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createSetUpProfileEvent(any(), any())(any())).thenReturn(fakeAuditEvent)

      val traderProfile = TraderProfile(testEori, "", None, None)
      val result        = await(auditService.auditProfileSetUp(traderProfile, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the trader profile to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createSetUpProfileEvent(eqTo(traderProfile), eqTo(AffinityGroup.Individual))(any())
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
        await(auditService.auditProfileSetUp(traderProfile, AffinityGroup.Individual))
      }

    }

  }

  "auditStartCreateGoodsRecord" - {

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createStartCreateGoodsRecord(any(), any())(any())).thenReturn(fakeAuditEvent)

      val result = await(auditService.auditStartCreateGoodsRecord(testEori, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the EORI and affinity group to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createStartCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual))(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createStartCreateGoodsRecord(any(), any())(any())).thenReturn(fakeAuditEvent)

      val result = await(auditService.auditStartCreateGoodsRecord(testEori, AffinityGroup.Individual))

      result mustBe Done

      withClue("Should have supplied the EORI and affinity group to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createStartCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual))(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {
      when(mockAuditConnector.sendEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        await(auditService.auditStartCreateGoodsRecord(testEori, AffinityGroup.Individual))
      }

    }

  }

  "auditFinishCreateGoodsRecord" - {

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createFinishCreateGoodsRecord(any(), any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val userAnswers         = generateUserAnswersForFinishCreateGoodsTest(true)
      val expectedGoodsRecord =
        GoodsRecord(
          testEori,
          "trader reference",
          testCommodity.commodityCode,
          "trader reference",
          "PF",
          testCommodity.validityStartDate,
          testCommodity.validityEndDate
        )

      val result = await(auditService.auditFinishCreateGoodsRecord(testEori, AffinityGroup.Individual, userAnswers))

      result mustBe Done

      withClue("Should have supplied the correct parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createFinishCreateGoodsRecord(
            eqTo(AffinityGroup.Individual),
            eqTo(expectedGoodsRecord),
            eqTo(testCommodity),
            eqTo(false)
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createFinishCreateGoodsRecord(any(), any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val userAnswers         = generateUserAnswersForFinishCreateGoodsTest(false)
      val expectedGoodsRecord =
        GoodsRecord(
          testEori,
          "trader reference",
          testCommodity.commodityCode,
          "goods description",
          "PF",
          testCommodity.validityStartDate,
          testCommodity.validityEndDate
        )

      val result = await(auditService.auditFinishCreateGoodsRecord(testEori, AffinityGroup.Individual, userAnswers))

      result mustBe Done

      withClue("Should have supplied the EORI and affinity group to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createFinishCreateGoodsRecord(
            eqTo(AffinityGroup.Individual),
            eqTo(expectedGoodsRecord),
            eqTo(testCommodity),
            eqTo(true)
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {
      when(mockAuditConnector.sendEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        await(
          auditService.auditFinishCreateGoodsRecord(
            testEori,
            AffinityGroup.Individual,
            generateUserAnswersForFinishCreateGoodsTest(true)
          )
        )
      }

    }

    "return Done when user answers are not sufficient to generate event" in {
      val userAnswers = emptyUserAnswers
      val result      = await(auditService.auditFinishCreateGoodsRecord(testEori, AffinityGroup.Individual, userAnswers))

      result mustBe Done

      withClue("Should not have tried to create the event as the details were invalid") {
        verify(mockAuditFactory, times(0))
          .createFinishCreateGoodsRecord(any, any, any, any)(any())
      }

      withClue("Should not have tried to submit an event to the audit connector") {
        verify(mockAuditConnector, times(0)).sendEvent(any)(any(), any())
      }

    }

  }

  "auditStartUpdateGoodsRecord" - {

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createStartUpdateGoodsRecord(any(), any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val result =
        await(
          auditService.auditStartUpdateGoodsRecord(
            testEori,
            AffinityGroup.Individual,
            "updateSection",
            testRecordId
          )
        )

      result mustBe Done

      withClue("Should have supplied the correct parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createStartUpdateGoodsRecord(
            eqTo(testEori),
            eqTo(AffinityGroup.Individual),
            eqTo("updateSection"),
            eqTo(testRecordId)
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }
    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = DataEvent("source", "type")
      when(mockAuditFactory.createStartUpdateGoodsRecord(any(), any(), any(), any())(any())).thenReturn(fakeAuditEvent)

      val result =
        await(
          auditService.auditStartUpdateGoodsRecord(
            testEori,
            AffinityGroup.Individual,
            "updateSection",
            testRecordId
          )
        )

      result mustBe Done

      withClue("Should have supplied the EORI and affinity group to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createStartUpdateGoodsRecord(
            eqTo(testEori),
            eqTo(AffinityGroup.Individual),
            eqTo("updateSection"),
            eqTo(testRecordId)
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {

      when(mockAuditConnector.sendEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        await(
          auditService.auditStartUpdateGoodsRecord(
            testEori,
            AffinityGroup.Individual,
            "updateSection",
            testRecordId
          )
        )
      }

    }

  }

  "auditValidateCommodityCode" - {

    val auditData = OttAuditData(
      testEori,
      AffinityGroup.Individual,
      Some(testRecordId),
      testCommodity.commodityCode,
      None,
      None,
      Some("CreateRecord")
    )
    val startTime = Instant.parse("2024-06-03T15:19:18.399Z")
    val endTime   = Instant.parse("2024-06-03T15:19:20.399Z")

    val responseBody = "responseBody"

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = ExtendedDataEvent("source", "type")
      when(
        mockAuditFactory.createValidateCommodityCodeEvent(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any())
      ).thenReturn(fakeAuditEvent)

      val result = await(
        auditService.auditValidateCommodityCode(
          Some(auditData),
          startTime,
          endTime,
          OK,
          Some(responseBody),
          Some(testCommodity)
        )
      )

      result mustBe Done

      withClue("Should have supplied the parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createValidateCommodityCodeEvent(
            eqTo(Some(auditData)),
            eqTo(startTime),
            eqTo(endTime),
            eqTo(OK),
            eqTo(Some(responseBody)),
            eqTo(Some(testCommodity))
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendExtendedEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = ExtendedDataEvent("source", "type")
      when(
        mockAuditFactory.createValidateCommodityCodeEvent(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any())
      ).thenReturn(fakeAuditEvent)

      val result = await(
        auditService.auditValidateCommodityCode(
          Some(auditData),
          startTime,
          endTime,
          OK,
          Some(responseBody),
          Some(testCommodity)
        )
      )

      result mustBe Done

      withClue("Should have supplied the parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createValidateCommodityCodeEvent(
            eqTo(Some(auditData)),
            eqTo(startTime),
            eqTo(endTime),
            eqTo(OK),
            eqTo(Some(responseBody)),
            eqTo(Some(testCommodity))
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendExtendedEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        await(
          auditService.auditValidateCommodityCode(
            Some(auditData),
            startTime,
            endTime,
            OK,
            Some(responseBody),
            Some(testCommodity)
          )
        )
      }

    }

  }

  "auditGetCategorisationAssessmentDetails" - {

    val auditData = OttAuditData(
      testEori,
      AffinityGroup.Individual,
      Some(testRecordId),
      testCommodity.commodityCode,
      Some("CX"),
      Some(LocalDate.now),
      None
    )
    val startTime = Instant.parse("2024-06-03T15:19:18.399Z")
    val endTime   = Instant.parse("2024-06-03T15:19:20.399Z")

    val responseBody    = "responseBody"
    val testOttResponse = OttResponse(
      GoodsNomenclatureResponse("1", testCommodity.commodityCode),
      Seq.empty[CategoryAssessmentRelationship],
      Seq.empty[IncludedElement]
    )

    "return Done when built up an audit event and submitted it" in {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val fakeAuditEvent = ExtendedDataEvent("source", "type")
      when(
        mockAuditFactory.createGetCategorisationAssessmentDetailsEvent(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any())
      ).thenReturn(fakeAuditEvent)

      val result = await(
        auditService.auditGetCategorisationAssessmentDetails(
          Some(auditData),
          startTime,
          endTime,
          OK,
          Some(responseBody),
          Some(testOttResponse)
        )
      )

      result mustBe Done

      withClue("Should have supplied the parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createGetCategorisationAssessmentDetailsEvent(
            eqTo(Some(auditData)),
            eqTo(startTime),
            eqTo(endTime),
            eqTo(OK),
            eqTo(Some(responseBody)),
            eqTo(Some(testOttResponse))
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendExtendedEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "return Done when audit return type is failure" in {

      val auditFailure = AuditResult.Failure("Failed audit event creation")
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(auditFailure))

      val fakeAuditEvent = ExtendedDataEvent("source", "type")
      when(
        mockAuditFactory.createGetCategorisationAssessmentDetailsEvent(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any())
      ).thenReturn(fakeAuditEvent)

      val result = await(
        auditService.auditGetCategorisationAssessmentDetails(
          Some(auditData),
          startTime,
          endTime,
          OK,
          Some(responseBody),
          Some(testOttResponse)
        )
      )

      result mustBe Done

      withClue("Should have supplied the parameters to the factory to create the event") {
        verify(mockAuditFactory, times(1))
          .createGetCategorisationAssessmentDetailsEvent(
            eqTo(Some(auditData)),
            eqTo(startTime),
            eqTo(endTime),
            eqTo(OK),
            eqTo(Some(responseBody)),
            eqTo(Some(testOttResponse))
          )(any())
      }

      withClue("Should have submitted the created event to the audit connector") {
        verify(mockAuditConnector, times(1)).sendExtendedEvent(eqTo(fakeAuditEvent))(any(), any())
      }

    }

    "must let the play error handler deal with an future failure" in {
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("audit error")))

      intercept[RuntimeException] {
        await(
          auditService.auditGetCategorisationAssessmentDetails(
            Some(auditData),
            startTime,
            endTime,
            OK,
            Some(responseBody),
            Some(testOttResponse)
          )
        )
      }

    }

  }

  private def generateUserAnswersForFinishCreateGoodsTest(useTraderRef: Boolean) = {
    val ua = emptyUserAnswers
      .set(CommodityQuery, testCommodity)
      .success
      .value
      .set(TraderReferencePage, "trader reference")
      .success
      .value
      .set(CountryOfOriginPage, "PF")
      .success
      .value
      .set(CommodityCodePage, testCommodity.commodityCode)
      .success
      .value
      .set(HasCorrectGoodsPage, true)
      .success
      .value
      .set(CommodityQuery, testCommodity)
      .success
      .value

    if (useTraderRef) {
      ua.set(UseTraderReferencePage, true).success.value
    } else {
      ua.set(UseTraderReferencePage, false)
        .success
        .value
        .set(GoodsDescriptionPage, "goods description")
        .success
        .value
    }
  }
}
