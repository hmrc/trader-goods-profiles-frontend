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

package controllers.goodsRecord

import base.SpecBase
import base.TestConstants.testEori
import controllers.routes
import navigation.{FakeNavigation, Navigation}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.CreateRecordStartView

import scala.concurrent.Future

class CreateRecordStartControllerSpec extends SpecBase {

  "CreateRecordStart Controller" - {

    "for a GET" - {
      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CreateRecordStartController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CreateRecordStartView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    "for a POST" - {
      "must redirect to the trader reference controller page" in {

        val onwardRoute = Call("", "")

        val mockAuditService = mock[AuditService]
        when(mockAuditService.auditStartCreateGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CreateRecordStartController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual onwardRoute.url

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService)
              .auditStartCreateGoodsRecord(eqTo(testEori), eqTo(AffinityGroup.Individual))(any())
          }

        }
      }

    }
  }
}
