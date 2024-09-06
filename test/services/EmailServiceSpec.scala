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
import connectors.{EmailConnector, NotificationConnector}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".sendDownloadRecordEmail" - {

    val testExpiredDate = "06 September 2024"
    val testEmail       = "someone@somewhere.com"

    val mockEmailConnector        = mock[EmailConnector]
    val mockNotificationConnector = mock[NotificationConnector]

    "should return Done when both connectors respond successfully" in {

      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockNotificationConnector.submitNotification(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val emailService = new EmailService(mockEmailConnector, mockNotificationConnector)

      val result = await(emailService.sendDownloadRecordEmail(testEori, testRecordId, testExpiredDate, testEmail))
      result shouldBe Done

      withClue("Should call Email Connector") {
        verify(mockEmailConnector).sendDownloadRecordEmail(any(), any())(any())
      }

      withClue("Should call Notification Connector") {
        verify(mockNotificationConnector).submitNotification(any(), any(), any())(any())
      }
    }

    "should throw an error when Email Connector fails" in {

      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Failed to send email")))

      val emailService = new EmailService(mockEmailConnector, mockNotificationConnector)

      val result = intercept[RuntimeException] {
        await(emailService.sendDownloadRecordEmail(testEori, testRecordId, testExpiredDate, testEmail))
      }

      result.getMessage shouldBe "Failed to send email"

      withClue("Should not call Notification Connector") {
        verify(mockNotificationConnector, never()).submitNotification(any(), any(), any())(any())
      }
    }

    "should throw an error when Notification Connector fails" in {

      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any()))
        .thenReturn(Future.successful(Done))
      when(mockNotificationConnector.submitNotification(any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Failed to send notification")))

      val emailService = new EmailService(mockEmailConnector, mockNotificationConnector)

      val result = intercept[RuntimeException] {
        await(emailService.sendDownloadRecordEmail(testEori, testRecordId, testExpiredDate, testEmail))
      }

      result.getMessage shouldBe "Failed to send notification"

      withClue("Should call Email Connector") {
        verify(mockEmailConnector).sendDownloadRecordEmail(any(), any())(any())
      }

      withClue("Should call Notification Connector and fails") {
        verify(mockNotificationConnector).submitNotification(any(), any(), any())(any())
      }
    }
  }
}
