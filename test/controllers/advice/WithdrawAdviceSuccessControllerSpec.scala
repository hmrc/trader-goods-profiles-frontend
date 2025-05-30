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

package controllers.advice

import base.SpecBase
import base.TestConstants.testRecordId
import connectors.TraderProfileConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.advice.WithdrawAdviceSuccessView

import scala.concurrent.Future

class WithdrawAdviceSuccessControllerSpec extends SpecBase with MockitoSugar {

  "WithdrawAdviceSuccess Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.advice.routes.WithdrawAdviceSuccessController.onPageLoad(testRecordId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WithdrawAdviceSuccessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId)(request, messages(application)).toString
        verify(mockTraderProfileConnector, never()).checkTraderProfile(any())(any())
      }
    }
  }
}
