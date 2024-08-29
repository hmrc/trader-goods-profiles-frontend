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
import base.TestConstants.{testEori, userAnswersId}
import connectors.TraderProfileConnector
import forms.{UkimsNumberFormProvider, UseExistingUkimsFormProvider}
import models.{NormalMode, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{UkimsNumberPage, UkimsNumberUpdatePage, UseExistingUkimsPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.{UkimsNumberView, UseExistingUkimsNumberView}

import scala.concurrent.Future

class UseExistingUkimsNumberControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new UseExistingUkimsFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  "onPageLoad" - {
    val ukimsNumberRoute = routes.UseExistingUkimsNumberController.onPageLoad().url

    "must return OK and the correct view for a GET" in {
      when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(false)

      val ukimsNumber = "UKIMS123"

      val userAnswers = UserAnswers(userAnswersId)
        .set(UkimsNumberPage, ukimsNumber)
        .success
        .value
        .set(UseExistingUkimsPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, ukimsNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UseExistingUkimsNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          routes.UseExistingUkimsNumberController.onSubmit(),
          ukimsNumber
        )(
          request,
          messages(application)
        ).toString
      }
    }

  }
}
