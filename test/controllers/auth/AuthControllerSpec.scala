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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.FakeAuthoriseAction
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import repositories.SessionRepository

import java.net.URLEncoder
import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val frontendAppConfig = mock[FrontendAppConfig]
  private val signOutUrl = "signOutUrl"
  private val exitSurvey = "exitSurvey"

  when(frontendAppConfig.signOutUrl) thenReturn signOutUrl
  when(frontendAppConfig.exitSurveyUrl) thenReturn exitSurvey

  private val sessionRepository = mock[SessionRepository]

  private val authController = new AuthController(
    messageComponentControllers,
    frontendAppConfig,
    sessionRepository,
    new FakeAuthoriseAction(defaultBodyParser)
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    reset(sessionRepository)

  }

  "signOut" - {

    "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL" in {

      when(sessionRepository.clear(any())) thenReturn Future.successful(true)

      val result = authController.signOut()(fakeRequest)

      val encodedContinueUrl  = URLEncoder.encode(exitSurvey, "UTF-8")
      val expectedRedirectUrl = s"$signOutUrl?continue=$encodedContinueUrl"

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual expectedRedirectUrl
      verify(sessionRepository, times(1)).clear(eqTo(userAnswersId))
    }
  }

  "signOutNoSurvey" - {

    "must clear users answers and redirect to sign out, specifying SignedOut as the continue URL" in {

      when(sessionRepository.clear(any())) thenReturn Future.successful(true)

      val result = authController.signOutNoSurvey()(fakeRequest)

      val encodedContinueUrl  = URLEncoder.encode(routes.SignedOutController.onPageLoad.url, "UTF-8")
      val expectedRedirectUrl = s"$signOutUrl?continue=$encodedContinueUrl"

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual expectedRedirectUrl
      verify(sessionRepository, times(1)).clear(eqTo(userAnswersId))
    }
  }
}
