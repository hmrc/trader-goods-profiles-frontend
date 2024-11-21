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

package controllers

import base.SpecBase
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService

import scala.concurrent.Future

class OutboundControllerSpec extends SpecBase {

  "Outbound Controller" - {

    "must redirect to the link" in {

      val mockAuditService = mock[AuditService]
      when(mockAuditService.auditOutboundClick(any(), any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.OutboundController.redirect("link", "linkText").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe "link"
      }
    }
  }
}
